/**
 * Paste into Figma MCP `use_figma` tool as the `code` argument.
 *
 * File: https://www.figma.com/design/dHEdCWNWsMkQI6yqX1uhLO
 * fileKey: dHEdCWNWsMkQI6yqX1uhLO
 *
 * Creates a 6×3 grid (states × concepts), adds column header labels, row labels,
 * repositions existing Connected frames (nodes 1:2, 1:21, 1:38), and builds
 * Loading / Unavailable / Idle / Connecting / Error variants per concept.
 *
 * Run once per file; running twice duplicates frames. To re-run, delete generated
 * frames first or use a fresh page.
 */

await figma.loadFontAsync({ family: 'Inter', style: 'Regular' })
await figma.loadFontAsync({ family: 'Inter', style: 'Semi Bold' })
await figma.loadFontAsync({ family: 'Inter', style: 'Bold' })

const page =
  figma.root.children.find((p) => p.name.includes('Voice agent')) || figma.root.children[0]
await figma.setCurrentPageAsync(page)

const FW = 360
const FH = 780
const GAP = 40
const ORIGIN_X = 72
const ORIGIN_Y = 140
const LABEL_GAP = 36
const ROW_LABEL_W = 220

function hex(c) {
  const n = String(c).replace('#', '')
  return {
    r: parseInt(n.slice(0, 2), 16) / 255,
    g: parseInt(n.slice(2, 4), 16) / 255,
    b: parseInt(n.slice(4, 6), 16) / 255,
  }
}
function solid(h, o = 1) {
  return [{ type: 'SOLID', color: hex(h), opacity: o }]
}

function makeText(str, size, style, colorHex, widthMax) {
  const t = figma.createText()
  t.fontName = { family: 'Inter', style }
  t.fontSize = size
  t.characters = str
  t.fills = solid(colorHex)
  if (widthMax) {
    t.textAutoResize = 'HEIGHT'
    t.resize(widthMax, t.height)
  }
  return t
}

function padShell(name, bg) {
  const f = figma.createAutoLayout('VERTICAL', { name, itemSpacing: 14 })
  f.resize(FW, FH)
  f.paddingTop = 20
  f.paddingBottom = 24
  f.paddingLeft = 20
  f.paddingRight = 20
  f.cornerRadius = 16
  f.fills = solid(bg)
  f.strokes = solid('#cbd5e1', 0.55)
  f.strokeWeight = 1
  return f
}

function infoBanner() {
  const row = figma.createAutoLayout('HORIZONTAL', { name: 'Disclaimer', itemSpacing: 10 })
  row.paddingTop = row.paddingBottom = 12
  row.paddingLeft = row.paddingRight = 12
  row.cornerRadius = 10
  row.fills = solid('#eff6ff')
  row.strokes = solid('#bfdbfe')
  row.strokeWeight = 1
  const icon = figma.createRectangle()
  icon.resize(18, 18)
  icon.cornerRadius = 4
  icon.fills = solid('#2563eb')
  row.appendChild(icon)
  const copy = figma.createAutoLayout('VERTICAL', { itemSpacing: 4 })
  copy.appendChild(makeText('Før du bruker stemme', 12, 'Semi Bold', '#1e293b', 280))
  copy.appendChild(
    makeText('KI – ikke Kevin. WebRTC til OpenAI. Økt ~3 min.', 11, 'Regular', '#475569', 280),
  )
  row.appendChild(copy)
  return row
}

function micOrbRing(live) {
  const wrap = figma.createAutoLayout('VERTICAL', { name: 'Orb', itemSpacing: 8 })
  wrap.primaryAxisAlignItems = 'CENTER'
  wrap.counterAxisAlignItems = 'CENTER'
  const e = figma.createEllipse()
  e.resize(150, 150)
  e.fills = solid('#2563eb')
  if (live) {
    e.strokes = solid('#60a5fa', 0.85)
    e.strokeWeight = 4
  }
  wrap.appendChild(e)
  return wrap
}

function micOrbSpinner() {
  const wrap = figma.createAutoLayout('VERTICAL', { name: 'OrbConnecting', itemSpacing: 8 })
  wrap.primaryAxisAlignItems = 'CENTER'
  wrap.counterAxisAlignItems = 'CENTER'
  const e = figma.createEllipse()
  e.resize(150, 150)
  e.fills = solid('#2563eb')
  const arc = figma.createEllipse()
  arc.resize(150, 150)
  arc.fills = []
  arc.strokes = solid('#ffffff', 0.92)
  arc.strokeWeight = 5
  arc.arcData = { startingAngle: -1.2, endingAngle: 1.2, innerRadius: 0.62 }
  const stack = figma.createFrame()
  stack.name = 'OrbStack'
  stack.resize(150, 150)
  stack.layoutMode = 'NONE'
  stack.fills = []
  stack.appendChild(e)
  arc.x = 0
  arc.y = 0
  stack.appendChild(arc)
  wrap.appendChild(stack)
  return wrap
}

function primaryBtn(label) {
  const b = figma.createAutoLayout('HORIZONTAL', { name: 'PrimaryBtn', itemSpacing: 8 })
  b.paddingTop = b.paddingBottom = 14
  b.paddingLeft = b.paddingRight = 28
  b.cornerRadius = 16
  b.fills = solid('#2563eb')
  b.primaryAxisAlignItems = 'CENTER'
  b.counterAxisAlignItems = 'CENTER'
  b.appendChild(makeText(label, 15, 'Semi Bold', '#ffffff'))
  return b
}

function secondaryDisabled(label) {
  const b = figma.createAutoLayout('HORIZONTAL', { name: 'SecondaryDisabled', itemSpacing: 8 })
  b.paddingTop = b.paddingBottom = 14
  b.paddingLeft = b.paddingRight = 28
  b.cornerRadius = 16
  b.fills = solid('#e2e8f0')
  b.primaryAxisAlignItems = 'CENTER'
  b.counterAxisAlignItems = 'CENTER'
  b.appendChild(makeText(label, 15, 'Semi Bold', '#64748b'))
  return b
}

function dangerOutlineBtn(label) {
  const b = figma.createAutoLayout('HORIZONTAL', { name: 'EndBtn', itemSpacing: 8 })
  b.paddingTop = b.paddingBottom = 14
  b.paddingLeft = b.paddingRight = 28
  b.cornerRadius = 16
  b.fills = solid('#ffffff')
  b.strokes = solid('#fecaca')
  b.strokeWeight = 1
  b.primaryAxisAlignItems = 'CENTER'
  b.counterAxisAlignItems = 'CENTER'
  b.appendChild(makeText(label, 15, 'Semi Bold', '#b91c1c'))
  return b
}

function loadingSpinnerBlock() {
  const wrap = figma.createAutoLayout('VERTICAL', { name: 'Loading', itemSpacing: 14 })
  wrap.primaryAxisAlignItems = 'CENTER'
  wrap.counterAxisAlignItems = 'CENTER'
  wrap.paddingTop = 120
  wrap.paddingBottom = 120
  const arc = figma.createEllipse()
  arc.resize(44, 44)
  arc.fills = []
  arc.strokes = solid('#2563eb', 0.95)
  arc.strokeWeight = 4
  arc.arcData = { startingAngle: -1.1, endingAngle: 1.1, innerRadius: 0.72 }
  wrap.appendChild(arc)
  wrap.appendChild(makeText('Sjekker stemmetilgang …', 13, 'Regular', '#475569', 280))
  return wrap
}

function unavailableBanner() {
  const box = figma.createAutoLayout('VERTICAL', { name: 'Unavailable', itemSpacing: 8 })
  box.paddingTop = box.paddingBottom = 12
  box.paddingLeft = box.paddingRight = 12
  box.cornerRadius = 14
  box.fills = solid('#fffbeb', 0.95)
  box.strokes = solid('#fcd34d', 0.9)
  box.strokeWeight = 1
  box.appendChild(
    makeText('Stemmechat er ikke slått på hos serveren akkurat nå.', 12, 'Semi Bold', '#78350f', 300),
  )
  return box
}

function errorModal() {
  const wrap = figma.createFrame()
  wrap.name = 'ErrorOverlay'
  wrap.resize(FW, FH)
  wrap.layoutMode = 'NONE'
  wrap.clipsContent = true
  wrap.fills = []

  const scrim = figma.createRectangle()
  scrim.resize(FW, FH)
  scrim.fills = [{ type: 'SOLID', color: hex('#0f172a'), opacity: 0.38 }]
  wrap.appendChild(scrim)

  const card = figma.createAutoLayout('VERTICAL', { name: 'Dialog', itemSpacing: 12 })
  card.paddingTop = card.paddingBottom = 18
  card.paddingLeft = card.paddingRight = 18
  card.cornerRadius = 14
  card.fills = solid('#ffffff')
  card.strokes = solid('#fecaca')
  card.strokeWeight = 1
  card.effects = [
    {
      type: 'DROP_SHADOW',
      color: { r: 0.06, g: 0.09, b: 0.16, a: 0.14 },
      offset: { x: 0, y: 10 },
      radius: 28,
      spread: -6,
      visible: true,
      blendMode: 'NORMAL',
    },
  ]

  const ic = figma.createAutoLayout('HORIZONTAL', { itemSpacing: 0 })
  ic.paddingTop = ic.paddingBottom = ic.paddingLeft = ic.paddingRight = 12
  ic.cornerRadius = 14
  ic.fills = solid('#ffffff')
  ic.strokes = solid('#fecaca')
  ic.strokeWeight = 1
  ic.appendChild(makeText('!', 14, 'Bold', '#dc2626', 16))

  card.appendChild(ic)
  card.appendChild(makeText('Stemme kunne ikke starte', 16, 'Bold', '#0f172a', 260))
  card.appendChild(
    makeText('Live AI-tjenesten trenger en ny økt før den kan fortsette.', 12, 'Regular', '#475569', 260),
  )
  const msg = figma.createAutoLayout('VERTICAL', { itemSpacing: 6 })
  msg.paddingTop = msg.paddingBottom = 10
  msg.paddingLeft = msg.paddingRight = 12
  msg.cornerRadius = 14
  msg.fills = solid('#ffffff', 0.9)
  msg.strokes = solid('#fecaca')
  msg.strokeWeight = 1
  msg.appendChild(makeText('Mikrofontilgang ble nektet.', 12, 'Semi Bold', '#b91c1c', 236))
  card.appendChild(msg)

  const footer = figma.createAutoLayout('HORIZONTAL', { name: 'Footer', itemSpacing: 10 })
  footer.primaryAxisAlignItems = 'CENTER'
  footer.counterAxisAlignItems = 'CENTER'
  footer.paddingTop = 6

  const ok = figma.createAutoLayout('HORIZONTAL', { itemSpacing: 6 })
  ok.paddingTop = ok.paddingBottom = 10
  ok.paddingLeft = ok.paddingRight = 18
  ok.cornerRadius = 10
  ok.fills = solid('#ffffff')
  ok.strokes = solid('#cbd5e1')
  ok.strokeWeight = 1
  ok.appendChild(makeText('OK', 13, 'Semi Bold', '#0f172a'))

  const retry = figma.createAutoLayout('HORIZONTAL', { itemSpacing: 6 })
  retry.paddingTop = retry.paddingBottom = 10
  retry.paddingLeft = retry.paddingRight = 18
  retry.cornerRadius = 10
  retry.fills = solid('#2563eb')
  retry.appendChild(makeText('Prøv igjen', 13, 'Semi Bold', '#ffffff'))

  footer.appendChild(ok)
  footer.appendChild(retry)
  card.appendChild(footer)

  wrap.appendChild(card)
  card.resizeWithoutConstraints(292, card.height)
  card.x = Math.round((FW - card.width) / 2)
  card.y = Math.round((FH - card.height) / 2)

  return wrap
}

function buildVoice01(state) {
  const shell = padShell('', '#f1f5f9')
  shell.clipsContent = true
  shell.appendChild(makeText('Snakk med Kevin sin AI', 22, 'Bold', '#1e293b', 310))
  shell.appendChild(
    makeText('Live stemme (GPT-Realtime). For dokument-chat → tekst.', 12, 'Regular', '#475569', 310),
  )
  shell.appendChild(makeText('Bruk tekstchat →', 12, 'Semi Bold', '#1d4ed8', 310))

  if (state === 'loading') {
    shell.appendChild(loadingSpinnerBlock())
    return shell
  }
  if (state === 'unavailable') {
    shell.appendChild(unavailableBanner())
    shell.appendChild(infoBanner())
    return shell
  }

  shell.appendChild(infoBanner())

  if (state === 'idle') {
    shell.appendChild(micOrbRing(false))
    shell.appendChild(primaryBtn('Start stemme'))
    return shell
  }
  if (state === 'connecting') {
    shell.appendChild(micOrbSpinner())
    shell.appendChild(secondaryDisabled('Kobler til…'))
    return shell
  }
  if (state === 'error') {
    shell.appendChild(micOrbRing(false))
    shell.appendChild(primaryBtn('Start stemme'))
    const overlay = errorModal()
    shell.appendChild(overlay)
    overlay.layoutPositioning = 'ABSOLUTE'
    overlay.x = 0
    overlay.y = 0
    return shell
  }
  return shell
}

function buildDash(state) {
  const shell = figma.createAutoLayout('VERTICAL', { name: '', itemSpacing: 12 })
  shell.resize(FW, FH)
  shell.paddingTop = 16
  shell.paddingBottom = 20
  shell.paddingLeft = shell.paddingRight = 16
  shell.cornerRadius = 16
  shell.fills = solid('#f8fafc')
  shell.strokes = solid('#cbd5e1', 0.55)
  shell.strokeWeight = 1
  shell.clipsContent = true

  shell.appendChild(makeText('Stemme · dashboard', 18, 'Bold', '#0f172a', 320))
  shell.appendChild(makeText('Status · transcript · hint på én rad', 11, 'Regular', '#64748b', 320))

  if (state === 'loading') {
    shell.appendChild(loadingSpinnerBlock())
    return shell
  }
  if (state === 'unavailable') {
    shell.appendChild(unavailableBanner())
    const thin = figma.createAutoLayout('HORIZONTAL', { itemSpacing: 8 })
    thin.paddingTop = thin.paddingBottom = 8
    thin.paddingLeft = thin.paddingRight = 10
    thin.cornerRadius = 8
    thin.fills = solid('#eff6ff')
    thin.appendChild(makeText('ⓘ', 12, 'Bold', '#2563eb', 20))
    thin.appendChild(makeText('KI • WebRTC • ~3 min økt', 11, 'Regular', '#334155', 240))
    shell.appendChild(thin)
    return shell
  }

  const split = figma.createAutoLayout('HORIZONTAL', { name: 'Split', itemSpacing: 12 })
  split.primaryAxisSizingMode = 'AUTO'
  split.counterAxisSizingMode = 'AUTO'

  const leftCol = figma.createAutoLayout('VERTICAL', { itemSpacing: 10 })
  leftCol.primaryAxisAlignItems = 'CENTER'
  const smOrb = figma.createEllipse()
  smOrb.resize(96, 96)
  smOrb.fills = solid('#4f46e5')
  if (state === 'connected') {
    smOrb.strokes = solid('#60a5fa', 0.85)
    smOrb.strokeWeight = 3
  }
  leftCol.appendChild(smOrb)

  if (state === 'idle') {
    leftCol.appendChild(makeText('Klar', 11, 'Semi Bold', '#64748b', 120))
    leftCol.appendChild(primaryBtn('Start stemme'))
  } else if (state === 'connecting') {
    const mini = figma.createEllipse()
    mini.resize(22, 22)
    mini.fills = []
    mini.strokes = solid('#ffffff', 0.95)
    mini.strokeWeight = 3
    mini.arcData = { startingAngle: -1.1, endingAngle: 1.1, innerRadius: 0.72 }
    leftCol.appendChild(mini)
    leftCol.appendChild(makeText('Kobler til …', 11, 'Semi Bold', '#475569', 140))
    leftCol.appendChild(secondaryDisabled('Kobler til…'))
  } else if (state === 'connected') {
    leftCol.appendChild(makeText('Live', 11, 'Semi Bold', '#16a34a', 120))
    leftCol.appendChild(primaryBtn('Start'))
  } else if (state === 'error') {
    leftCol.appendChild(makeText('Feil', 11, 'Semi Bold', '#b91c1c', 120))
    leftCol.appendChild(primaryBtn('Start stemme'))
  }

  const rightCol = figma.createAutoLayout('VERTICAL', { itemSpacing: 8 })
  const showTx = state === 'connected'
  rightCol.appendChild(makeText('Du', 10, 'Semi Bold', '#64748b', 150))
  rightCol.appendChild(makeText(showTx ? 'Hei …' : '…', 12, 'Regular', '#1e293b', 150))
  rightCol.appendChild(makeText('AI', 10, 'Semi Bold', '#64748b', 150))
  rightCol.appendChild(makeText(showTx ? 'Svar …' : '…', 12, 'Regular', '#1e293b', 150))

  split.appendChild(leftCol)
  split.appendChild(rightCol)
  shell.appendChild(split)

  const thin = figma.createAutoLayout('HORIZONTAL', { itemSpacing: 8 })
  thin.paddingTop = thin.paddingBottom = 8
  thin.paddingLeft = thin.paddingRight = 10
  thin.cornerRadius = 8
  thin.fills = solid('#eff6ff')
  thin.appendChild(makeText('ⓘ', 12, 'Bold', '#2563eb', 20))
  thin.appendChild(makeText('KI • WebRTC • ~3 min økt', 11, 'Regular', '#334155', 240))
  shell.appendChild(thin)

  if (state === 'error') {
    const overlay = errorModal()
    shell.appendChild(overlay)
    overlay.layoutPositioning = 'ABSOLUTE'
    overlay.x = 0
    overlay.y = 0
  }

  return shell
}

function minimalFoot() {
  const foot = figma.createAutoLayout('VERTICAL', { itemSpacing: 6 })
  foot.primaryAxisAlignItems = 'CENTER'
  foot.counterAxisAlignItems = 'CENTER'
  foot.appendChild(makeText('Tekstchat for dypere svar', 11, 'Semi Bold', '#2563eb', 260))
  foot.appendChild(makeText('KI • ikke person • ~3 min', 10, 'Regular', '#94a3b8', 260))
  return foot
}

function buildMinimal(state) {
  const shell = padShell('', '#fafafa')
  shell.primaryAxisAlignItems = 'CENTER'
  shell.counterAxisAlignItems = 'CENTER'
  shell.itemSpacing = 20
  shell.clipsContent = true

  shell.appendChild(makeText('Snakk', 28, 'Bold', '#0f172a', 300))
  shell.appendChild(makeText('Én primær handling · rolig bakgrunn', 12, 'Regular', '#64748b', 280))

  if (state === 'loading') {
    shell.appendChild(loadingSpinnerBlock())
    shell.appendChild(minimalFoot())
    return shell
  }

  if (state === 'unavailable') {
    shell.appendChild(unavailableBanner())
    shell.appendChild(minimalFoot())
    return shell
  }

  const foot = minimalFoot()

  if (state === 'connecting') {
    const big = figma.createEllipse()
    big.resize(200, 200)
    big.fills = solid('#1e40af')
    const arc = figma.createEllipse()
    arc.resize(200, 200)
    arc.fills = []
    arc.strokes = solid('#ffffff', 0.92)
    arc.strokeWeight = 6
    arc.arcData = { startingAngle: -1.2, endingAngle: 1.2, innerRadius: 0.62 }
    const stack = figma.createFrame()
    stack.name = 'OrbStack'
    stack.resize(200, 200)
    stack.layoutMode = 'NONE'
    stack.fills = []
    stack.appendChild(big)
    arc.x = 0
    arc.y = 0
    stack.appendChild(arc)
    shell.appendChild(stack)
    shell.appendChild(secondaryDisabled('Kobler til…'))
    shell.appendChild(foot)
    return shell
  }

  const big = figma.createEllipse()
  big.resize(200, 200)
  big.fills = solid('#1e40af')
  if (state === 'connected') {
    big.strokes = solid('#60a5fa', 0.85)
    big.strokeWeight = 4
  }
  shell.appendChild(big)

  if (state === 'idle') shell.appendChild(primaryBtn('Start stemme'))
  if (state === 'connected') {
    shell.appendChild(makeText('Aktiv · ~3 min', 12, 'Semi Bold', '#15803d', 300))
    shell.appendChild(dangerOutlineBtn('Avslutt'))
  }
  if (state === 'error') shell.appendChild(primaryBtn('Start stemme'))

  shell.appendChild(foot)

  if (state === 'error') {
    const overlay = errorModal()
    shell.appendChild(overlay)
    overlay.layoutPositioning = 'ABSOLUTE'
    overlay.x = 0
    overlay.y = 0
  }

  return shell
}

const createdIds = []
const mutatedIds = []

const states = ['loading', 'unavailable', 'idle', 'connecting', 'connected', 'error']
const colLabels = ['Loading', 'Unavailable', 'Idle', 'Connecting', 'Connected', 'Error']
const rowLabels = ['01 · Dagens VoiceView', '02 · Dashboard', '03 · Minimal fokus']

const header = figma.createAutoLayout('HORIZONTAL', { name: 'State columns', itemSpacing: GAP })
header.primaryAxisAlignItems = 'MIN'
header.counterAxisAlignItems = 'CENTER'
const spacer = figma.createFrame()
spacer.name = 'Row label spacer'
spacer.resize(ROW_LABEL_W, 28)
spacer.fills = []
header.appendChild(spacer)
for (const lab of colLabels) {
  const cell = figma.createAutoLayout('VERTICAL', { itemSpacing: 4 })
  cell.resize(FW, 28)
  cell.primaryAxisAlignItems = 'CENTER'
  cell.counterAxisAlignItems = 'CENTER'
  cell.appendChild(makeText(lab, 11, 'Semi Bold', '#334155', FW))
  header.appendChild(cell)
}
page.appendChild(header)
header.x = ORIGIN_X
header.y = ORIGIN_Y - LABEL_GAP
createdIds.push(header.id)

const connectedIds = ['1:2', '1:21', '1:38']

for (let row = 0; row < 3; row++) {
  const rowHead = makeText(rowLabels[row], 11, 'Semi Bold', '#0f172a', ROW_LABEL_W)
  rowHead.textAlignHorizontal = 'RIGHT'
  page.appendChild(rowHead)
  rowHead.x = ORIGIN_X
  rowHead.y = ORIGIN_Y + row * (FH + GAP) + FH / 2 - 8
  createdIds.push(rowHead.id)

  for (let col = 0; col < 6; col++) {
    const st = states[col]
    const x = ORIGIN_X + ROW_LABEL_W + GAP + col * (FW + GAP)
    const y = ORIGIN_Y + row * (FH + GAP)

    if (st === 'connected') {
      const node = figma.getNodeById(connectedIds[row])
      if (!node || node.type !== 'FRAME') throw new Error('Missing connected frame row ' + row)
      node.name = `${rowLabels[row]} · ${colLabels[col]}`
      node.x = x
      node.y = y
      mutatedIds.push(node.id)
      continue
    }

    let frame
    if (row === 0) frame = buildVoice01(st)
    else if (row === 1) frame = buildDash(st)
    else frame = buildMinimal(st)

    frame.name = `${rowLabels[row]} · ${colLabels[col]}`
    page.appendChild(frame)
    frame.x = x
    frame.y = y
    createdIds.push(frame.id)
  }
}

return {
  ok: true,
  createdNodeIds: createdIds,
  mutatedNodeIds: mutatedIds,
  fileUrl: 'https://www.figma.com/design/dHEdCWNWsMkQI6yqX1uhLO',
}
