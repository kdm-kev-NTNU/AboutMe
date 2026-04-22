# AboutMe portfolio (seed)

This file is bundled in the API JAR so PostgreSQL + pgvector can be smoke-tested after deploy.
It is re-ingested when an admin calls `POST /admin/tools/documents/reseed`.

- Stack: Spring Boot, Vue, PostgreSQL with pgvector (Spring AI).
- Retrieval uses embeddings (e.g. text-embedding-3-large) stored in the `vector_store` table.

To replace this seed with a custom CV or project notes, use supported formats (for example `.md`, `.pdf`) and run reseed or upload through the admin document pipeline.
