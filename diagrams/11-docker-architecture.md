# 11 — Docker / Docker Compose Architecture

```mermaid
flowchart TB
    subgraph NET["Docker network: shopsphere-net"]
        subgraph APP["profile: app"]
            FE["frontend<br/>nginx-unprivileged :8080 → host 3000"]
            BE["backend<br/>JRE 21 :8080 → host 8080"]
            PG[("postgres :5432")]
        end
        subgraph AIP["profile: ai"]
            OL["ollama :11434"]
        end
        subgraph CI["profile: ci"]
            JK["jenkins :8080 → host 8085"]
            SQ["sonarqube :9000"]
            NX["nexus :8081 / :8082"]
        end
        subgraph MON["profile: monitoring"]
            PR["prometheus :9090"]
            GR["grafana :3000 → host 3001"]
            AM["alertmanager :9093"]
            NE["node-exporter :9100"]
            MP["mailpit :8025"]
        end
    end
    FE -->|"/api proxy"| BE
    BE --> PG
    BE --> OL
    PR -->|"scrape"| BE & NE
    PR --> AM --> MP
    GR --> PR
    PG --- V1[("volume pgdata")]
    JK --- V2[("volume jenkins_home")]
    SQ --- V3[("volume sonarqube_data")]
    NX --- V4[("volume nexus_data")]
```

Multi-stage builds: backend = Maven/JDK build stage → JRE runtime; frontend = Node build
stage → Nginx runtime. Both run as non-root with a HEALTHCHECK.
