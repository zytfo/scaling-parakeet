# Alert Term Extraction

This is a solution for an Alert Term Extraction task.

## Stack

- **Java 21**, **Spring Boot 3.4.5**, **Maven**, **Docker**
- **Spring Web** - REST controller + RestClient for API calls
- **Spring Boot Actuator** - health checks for deployment
- **Spring Validation** - input parameter validation (`@Min`, `@Max`) for batches
- **springdoc-openapi** - Swagger UI at `/docs`
- **Deployed at [Render](https://scaling-parakeet-5hdn.onrender.com/)** - may take a while to spin up after some inactivity 

## How It Works

The application queries two remote endpoints:
- **`/testQueryTerm`** - returns a deterministic list of query terms which are loaded once at startup. Terms are tokenized and cached in-memory using `@PostConstruct`.
- **`/testAlerts`** - returns alert objects, non-deterministic, has no limit tho - no need for a timeout between requests.

For each alert the service checks every query term against each content entry and returns the matches based on query parameters (the number of `batches` and `strictLanguage` - to check (or not) for the same language).

### Algorithm

**1. Tokenize**: split alert content into lowercase tokens using Unicode regex `[^\p{L}\p{N}]+`.

```
"Workers of ig metall, unite!" ->  ["workers", "of", "ig", "metall", "unite"]
"#StrikeForBlackLives"         ->  ["strikeforblacklives"]
"Arbeitsplätze"                ->  ["arbeitsplätze"]
```

**2. Match**: two modes based on the `keepOrder` flag:

- **`keepOrder=true`**: term tokens must appear next to each other. Uses a sliding window algorithm:
  ```
  Query term: "IG Metall" -> ["ig", "metall"]
  Alert content: "Wolfgang Lemb ig metall Germany"

  [wolfgang, lemb] -> NO
  [lemb, ig] -> NO
  [ig, metall] -> YES
  ```

- **`keepOrder=false`**: every term token must exist somewhere in the text:
  ```
  Query term: "climate change" -> ["climate", "change"]
  Alert content: "The change in global climate is alarming"

  Contains "climate"? YES
  Contains "change"? YES
  => MATCH
  ```

**3. Language filter**: with `strictLanguage=true` (by default), a query term only matches content with the same language code.

**4. Deduplicate**: each `(alertId, queryTermId)` pair appears at most once in results.

**Complexity**:
```
O(A * C * Q * T)

A - number of alerts
C - contents per alert
Q - query terms
T - tokens per content text
```

## API Endpoints

| Method | Path | Description                                              |
|---|---|----------------------------------------------------------|
| `GET` | `/api/v1/matches?batches=100&strictLanguage=true` | Fetch alert batches and match against cached query terms |
| `GET` | `/api/v1/query-terms` | Return cached query terms (not tokenized)                |
| `GET` | `/actuator/health` | Health check                                             |
| `GET` | `/docs` | Swagger UI                                               |

### Response format

```json
{
  "matches": [
    {
      "alertId": "6gbujhu89786",
      "queryTermId": 101,
      "queryTermText": "IG Metall",
      "matchedInContent": "Wolfgang Lemb, ig metall Germany stands in solidarity",
      "contentLanguage": "de"
    }
  ],
  "totalMatches": 1,
  "alertsFetched": 5,
  "queryTermsUsed": 30,
  "strictLanguageMatch": true
}
```

### Parameters

| Parameter | Default | Range | Description                                             |
|---|---|---|---------------------------------------------------------|
| `batches` | 100 | 1-1000 | Number of batches to fetch                              |
| `strictLanguage` | true | - | Match query terms with the same language as the content |

## Design Decisions

### API key in query parameters

Currently, Prewave requires the API key as a query parameter, which seems to be done for discussion. Instead, the API key may go in headers (for instance, `X-API-Key`) for security reasons.

### In-memory caching of query terms

The `/testQueryTerm` response not that big and deterministic, so it's loaded once at startup using `@PostConstruct` (which ensures the server doesn't accept traffic until query terms are ready) and cached in memory. 
For higher availability, query terms could be cached in Redis to survive restarts without requiring the API to be reachable.

## Running Locally

```bash
# Copy .env.example to .env and add PREWAVE_API_BASE_URL and PREWAVE_API_KEY as variables
cp .env.example .env

# Run
export $(cat .env | xargs) && ./mvnw spring-boot:run

# Access docs
open http://localhost:8080/docs
```

## Docker

```bash
docker build -t alert-term-extraction .

docker run -p 8080:8080 --env-file .env alert-term-extraction
```

## Possible Improvements

- **Stemming/lemmatization**: match various versions of the words ("changing" -> "change").
- **Proximity search**: for `keepOrder=false` would be good for term parts to appear within N tokens of each other, not just anywhere in the text - depending on business requirements.
- **Parallel fetching of alert batches**: run HTTP calls concurrently instead of sequentially for lower latency. As `/testAlerts` has no limits, may not to be kept in mind. 
