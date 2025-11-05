# Configuração de Ambientes

Este documento explica como configurar e executar o projeto em diferentes ambientes.

## 📋 Visão Geral

O projeto suporta dois perfis (profiles) de configuração:
- **`docker`** (padrão): Para desenvolvimento local com Kafka e PostgreSQL em containers Docker
- **`local`**: Para ambiente empresarial com Kafka e PostgreSQL externos

## 🏢 Ambiente Empresarial (Perfil `local`)

### 1. Configurar Conexões

Edite os seguintes ficheiros com as informações do seu ambiente empresarial:

#### Producer: `producer-app/src/main/resources/application-local.yaml`

```yaml
spring:
  datasource:
    url: jdbc:postgresql://seu-servidor-postgres:5432/nome-da-base
    username: seu-utilizador
    password: sua-password
  
  kafka:
    bootstrap-servers: seu-servidor-kafka:9092
```

#### Consumer: `consumer-app/src/main/resources/application-local.yaml`

```yaml
spring:
  datasource:
    url: jdbc:postgresql://seu-servidor-postgres:5432/nome-da-base
    username: seu-utilizador
    password: sua-password
  
  kafka:
    bootstrap-servers: seu-servidor-kafka:9092
```

### 2. Executar com Perfil Local

**Opção 1: Via linha de comando**
```bash
# Producer
cd producer-app
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Consumer
cd consumer-app
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

**Opção 2: Via variável de ambiente**
```bash
export SPRING_PROFILES_ACTIVE=local

# Producer
cd producer-app
mvn spring-boot:run

# Consumer
cd consumer-app
mvn spring-boot:run
```

**Opção 3: Via JAR executável**
```bash
# Build
mvn clean package -DskipTests

# Executar
java -jar -Dspring.profiles.active=local producer-app/target/producer-app-0.0.1-SNAPSHOT.jar
java -jar -Dspring.profiles.active=local consumer-app/target/consumer-app-0.0.1-SNAPSHOT.jar
```

## 🐳 Ambiente Docker Local (Perfil `docker`)

### 1. Iniciar Infraestrutura

```bash
docker-compose up -d
```

Isto inicia:
- Kafka (porta 9092)
- PostgreSQL (porta 5432)
- Prometheus (porta 9090)
- Grafana (porta 3000)

### 2. Executar Aplicações

O perfil `docker` é o padrão, por isso não precisa especificar:

```bash
# Producer
cd producer-app
mvn spring-boot:run

# Consumer
cd consumer-app
mvn spring-boot:run
```

Ou explicitamente:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=docker
```

### 3. Parar Infraestrutura

```bash
docker-compose down
```

## 🔍 Verificar Perfil Activo

Ao iniciar a aplicação, verifique nos logs:

```
The following profiles are active: docker
```

ou

```
The following profiles are active: local
```

## ⚠️ Notas Importantes

1. **Variáveis de Ambiente**: Pode sobrescrever qualquer propriedade usando variáveis de ambiente:
   ```bash
   export SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka.empresa:9092
   export SPRING_DATASOURCE_URL=jdbc:postgresql://db.empresa:5432/kafkadb
   mvn spring-boot:run
   ```

2. **Múltiplos Consumers**: Ao executar múltiplas instâncias do consumer, use portas diferentes:
   ```bash
   # Consumer 1
   mvn spring-boot:run -Dspring-boot.run.profiles=local
   
   # Consumer 2
   mvn spring-boot:run -Dspring-boot.run.profiles=local -Dspring-boot.run.arguments="--server.port=8082"
   
   # Consumer 3
   mvn spring-boot:run -Dspring-boot.run.profiles=local -Dspring-boot.run.arguments="--server.port=8083"
   ```

3. **Segurança**: **NÃO** comite credenciais reais nos ficheiros `application-local.yaml`. Use:
   - Variáveis de ambiente
   - Ficheiros de configuração externos (via `--spring.config.location`)
   - Gestores de secrets (Vault, AWS Secrets Manager, etc.)

## 🎯 Checklist de Configuração

### Para Ambiente Empresarial

- [ ] Obter endereço do servidor Kafka empresarial
- [ ] Obter endereço do servidor PostgreSQL empresarial
- [ ] Obter credenciais da base de dados
- [ ] Editar `application-local.yaml` em ambas as apps (producer e consumer)
- [ ] Testar conectividade ao Kafka: `telnet seu-kafka:9092`
- [ ] Testar conectividade ao PostgreSQL: `psql -h seu-db -U utilizador -d kafkadb`
- [ ] Executar producer com perfil `local`
- [ ] Executar consumer com perfil `local`
- [ ] Publicar mensagem de teste via API
- [ ] Verificar logs de ambas as aplicações

### Para Ambiente Docker

- [ ] Docker e Docker Compose instalados
- [ ] Executar `docker-compose up -d`
- [ ] Aguardar ~30 segundos para infraestrutura iniciar
- [ ] Verificar containers: `docker-compose ps`
- [ ] Executar producer (perfil docker é padrão)
- [ ] Executar consumer (perfil docker é padrão)
- [ ] Publicar mensagem de teste via API
- [ ] Verificar logs de ambas as aplicações

## 📞 Troubleshooting

### Erro: Connection refused ao Kafka
- Verificar se o servidor Kafka está acessível
- Confirmar porta e hostname corretos em `application-local.yaml`
- Testar conectividade: `telnet seu-kafka 9092`

### Erro: Connection refused ao PostgreSQL
- Verificar se o servidor PostgreSQL está acessível
- Confirmar credenciais e URL em `application-local.yaml`
- Testar conectividade: `psql -h seu-db -U utilizador -d kafkadb`

### Perfil errado está a ser usado
- Verificar variável de ambiente: `echo $SPRING_PROFILES_ACTIVE`
- Verificar logs de startup para confirmar perfil activo
- Limpar variáveis: `unset SPRING_PROFILES_ACTIVE`

### Propriedades não são aplicadas
- Ordem de precedência do Spring Boot:
  1. Variáveis de ambiente
  2. Argumentos de linha de comando
  3. application-{profile}.yaml
  4. application.yaml
