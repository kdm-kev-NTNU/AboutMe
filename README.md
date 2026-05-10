# AboutMe

Personlig nettside og portfolio med tekstchat og stemme som kan svare ut fra offentlig innhold om utdanning, prosjekter og erfaring. Grensesnittet finnes på norsk og engelsk.

## Hva du finner på siden

- **Portfolio** — oversikt over prosjekter, karriere og bakgrunn  
- **Chat** — still spørsmål om innhold som er lagt inn som kunnskapsgrunnlag  
- **Stemme** — snakk med assistenten der det er slått på i utvikling  
- **Tilbakemelding** — send inn kommentarer eller forslag  

Personvern og informasjonskapsler er forklart på siden.

## Teknologi (kort fortalt)

Nettsiden er bygget som en moderne webapp: Vue på brukerflaten, Java/Spring på serveren, og en database med vektorlagring slik at svarene kan hente inn relevant tekst før de genereres.

## Kjøre prosjektet lokalt

Du trenger Docker og en OpenAI API-nøkkel.

1. Kopier [`.env.example`](.env.example) til `backend/.env` og sett `OPENAI_API_KEY`.  
2. Kjør:

```bash
docker compose up -d --build
```

Typisk så åpner du appen på [http://localhost:5173](http://localhost:5173). Mer detaljer for utvikling finnes i [frontend/homepage/README.md](frontend/homepage/README.md).

## Kontakt

Spørsmål eller tilbakemeldinger: [kevindmazali@gmail.com](mailto:kevindmazali@gmail.com)
