package com.kevinmazali.portfolio.service;

import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.djl.util.PairList;
import com.kevinmazali.portfolio.config.RetrievalProperties;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.lang.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import ai.onnxruntime.NodeInfo;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OnnxValue;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;

/**
 * Cross-encoder style relevance scoring using an ONNX classification model and HF tokenizers.
 */
@Slf4j
public final class OnnxCrossEncoderReranker implements DocumentReranker, DisposableBean {

  private final RetrievalProperties props;
  @Nullable private final MeterRegistry meterRegistry;
  private final ReentrantLock loadLock = new ReentrantLock();
  private volatile OrtSession session;
  private volatile HuggingFaceTokenizer tokenizer;
  private volatile String inputIdsName = "input_ids";
  private volatile String attentionMaskName = "attention_mask";
  private volatile String tokenTypeIdsName = "token_type_ids";
  private volatile boolean useTokenTypeIds = true;
  private volatile String logitsOutputName = "logits";

  public OnnxCrossEncoderReranker(RetrievalProperties props, @Nullable MeterRegistry meterRegistry) {
    this.props = props;
    this.meterRegistry = meterRegistry;
  }

  @Override
  public List<Document> rerank(String query, List<Document> candidates, int topN) {
    if (candidates == null || candidates.isEmpty() || topN <= 0) {
      return List.of();
    }
    String q = query == null ? "" : query;
    Timer.Sample sample =
        meterRegistry != null ? Timer.start(meterRegistry) : null;
    try {
      ensureLoaded();
      OrtSession s = session;
      HuggingFaceTokenizer tok = tokenizer;
      if (s == null || tok == null) {
        return new PassThroughDocumentReranker().rerank(q, candidates, topN);
      }

      int batchSize = Math.max(1, props.getRerankBatchSize());
      int maxPassage = Math.max(256, props.getMaxPassageChars());
      List<ScoredDocument> scored = new ArrayList<>(candidates.size());

      for (int start = 0; start < candidates.size(); start += batchSize) {
        int end = Math.min(start + batchSize, candidates.size());
        PairList<String, String> pairs = new PairList<>();
        List<Document> batchDocs = new ArrayList<>(end - start);
        for (int i = start; i < end; i++) {
          Document d = candidates.get(i);
          String passage = truncate(d.getText(), maxPassage);
          pairs.add(q, passage);
          batchDocs.add(d);
        }
        Encoding[] encodings = tok.batchEncode(pairs);
        float[] batchScores = runBatch(s, encodings);
        for (int i = 0; i < batchDocs.size(); i++) {
          scored.add(new ScoredDocument(batchDocs.get(i), batchScores[i]));
        }
      }

      scored.sort(Comparator.comparingDouble(ScoredDocument::score).reversed());
      return scored.stream().limit(topN).map(ScoredDocument::doc).toList();
    } catch (Exception e) {
      log.warn("ONNX rerank failed ({}); using merge order for top {}", e.getMessage(), topN);
      return new PassThroughDocumentReranker().rerank(q, candidates, topN);
    } finally {
      if (sample != null && meterRegistry != null) {
        sample.stop(
            Timer.builder("portfolio.retrieval.rerank")
                .description("Local ONNX cross-encoder rerank latency")
                .register(meterRegistry));
      }
    }
  }

  private void ensureLoaded() throws Exception {
    if (session != null && tokenizer != null) {
      return;
    }
    loadLock.lock();
    try {
      if (session != null && tokenizer != null) {
        return;
      }
      Path modelFile = Paths.get(props.getOnnxModelPath());
      if (!Files.isRegularFile(modelFile)) {
        throw new IllegalStateException("ONNX model file not found: " + modelFile.toAbsolutePath());
      }
      String tokenizerSetting = props.getTokenizerPath();
      Path tokenizerRoot =
          (tokenizerSetting == null || tokenizerSetting.isBlank())
              ? modelFile.getParent()
              : Paths.get(tokenizerSetting);
      if (!Files.exists(tokenizerRoot)) {
        throw new IllegalStateException("Tokenizer path not found: " + tokenizerRoot.toAbsolutePath());
      }

      Map<String, String> tokOpts = new HashMap<>();
      tokOpts.put("maxLength", String.valueOf(Math.max(32, props.getMaxSequenceLength())));
      tokOpts.put("truncation", "LONGEST_FIRST");
      tokOpts.put("padding", "MAX_LENGTH");

      HuggingFaceTokenizer tok = HuggingFaceTokenizer.newInstance(tokenizerRoot, tokOpts);
      OrtEnvironment env = OrtEnvironment.getEnvironment();
      OrtSession.SessionOptions sessionOpts = new OrtSession.SessionOptions();
      OrtSession sess = env.createSession(modelFile.toAbsolutePath().toString(), sessionOpts);
      resolveNames(sess);

      this.tokenizer = tok;
      this.session = sess;
      log.info(
          "Loaded ONNX reranker session inputs={} output={}",
          sess.getInputInfo().keySet(),
          logitsOutputName);
    } finally {
      loadLock.unlock();
    }
  }

  private void resolveNames(OrtSession sess) throws OrtException {
    Map<String, NodeInfo> inputs = sess.getInputInfo();
    for (String name : inputs.keySet()) {
      String n = name.toLowerCase(Locale.ROOT);
      if (n.contains("input") && n.contains("id")) {
        inputIdsName = name;
      } else if (n.contains("attention")) {
        attentionMaskName = name;
      } else if (n.contains("token") && n.contains("type")) {
        tokenTypeIdsName = name;
      }
    }
    useTokenTypeIds = inputs.containsKey(tokenTypeIdsName);

    Map<String, NodeInfo> outputs = sess.getOutputInfo();
    if (outputs.containsKey("logits")) {
      logitsOutputName = "logits";
    } else if (!outputs.isEmpty()) {
      logitsOutputName = outputs.keySet().iterator().next();
    }
  }

  private float[] runBatch(OrtSession sess, Encoding[] encodings) throws OrtException {
    int batch = encodings.length;
    if (batch == 0) {
      return new float[0];
    }
    int seqLen = encodings[0].getIds().length;
    long[][] inputIds = new long[batch][seqLen];
    long[][] attentionMask = new long[batch][seqLen];
    long[][] tokenTypeIds = new long[batch][seqLen];
    for (int b = 0; b < batch; b++) {
      long[] ids = encodings[b].getIds();
      long[] attn = encodings[b].getAttentionMask();
      long[] types = encodings[b].getTypeIds();
      if (ids.length != seqLen || attn.length != seqLen) {
        throw new IllegalStateException(
            "Unexpected encoding length batch=" + b + " ids=" + ids.length + " expected=" + seqLen);
      }
      System.arraycopy(ids, 0, inputIds[b], 0, seqLen);
      System.arraycopy(attn, 0, attentionMask[b], 0, seqLen);
      if (types != null && types.length == seqLen) {
        System.arraycopy(types, 0, tokenTypeIds[b], 0, seqLen);
      }
    }

    OrtEnvironment env = OrtEnvironment.getEnvironment();
    List<OnnxTensor> tensors = new ArrayList<>(3);
    try {
      tensors.add(OnnxTensor.createTensor(env, inputIds));
      tensors.add(OnnxTensor.createTensor(env, attentionMask));
      Map<String, OnnxTensor> feeds = new HashMap<>();
      feeds.put(inputIdsName, tensors.get(0));
      feeds.put(attentionMaskName, tensors.get(1));
      if (useTokenTypeIds) {
        tensors.add(OnnxTensor.createTensor(env, tokenTypeIds));
        feeds.put(tokenTypeIdsName, tensors.get(2));
      }
      try (OrtSession.Result result = sess.run(feeds)) {
        OnnxValue value = pickLogitsOutput(result);
        return scoresFromLogits(value);
      }
    } finally {
      for (OnnxTensor t : tensors) {
        if (t != null) {
          t.close();
        }
      }
    }
  }

  private OnnxValue pickLogitsOutput(OrtSession.Result result) throws OrtException {
    OnnxValue named = result.get(logitsOutputName).orElse(null);
    if (named != null) {
      return named;
    }
    return result.get(0);
  }

  private static float[] scoresFromLogits(OnnxValue value) throws OrtException {
    Object raw = ((OnnxTensor) value).getValue();
    if (raw instanceof float[][] logits2) {
      float[] scores = new float[logits2.length];
      for (int b = 0; b < logits2.length; b++) {
        scores[b] = (float) scoreRow(logits2[b]);
      }
      return scores;
    }
    if (raw instanceof float[] logits1) {
      return logits1.clone();
    }
    throw new IllegalStateException("Unexpected logits type: " + raw.getClass());
  }

  private static double scoreRow(float[] row) {
    if (row == null || row.length == 0) {
      return 0.0;
    }
    if (row.length == 1) {
      return row[0];
    }
    return row[1] - row[0];
  }

  private static String truncate(String text, int maxChars) {
    if (text == null) {
      return "";
    }
    if (text.length() <= maxChars) {
      return text;
    }
    return text.substring(0, maxChars);
  }

  @Override
  public void destroy() {
    loadLock.lock();
    try {
      if (session != null) {
        try {
          session.close();
        } catch (Exception e) {
          log.debug("Error closing ONNX session: {}", e.getMessage());
        }
        session = null;
      }
      tokenizer = null;
    } finally {
      loadLock.unlock();
    }
  }

  private record ScoredDocument(Document doc, double score) {}
}
