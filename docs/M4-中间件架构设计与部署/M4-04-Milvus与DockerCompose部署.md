# M4-04: Milvus 向量库与 Docker Compose 完整部署

## 第一部分：Milvus 向量库设计

### 1. Milvus 集群架构

```
[Milvus Standalone / Cluster]
   ├─ Proxy（接收 SDK 请求）
   ├─ Query Node（向量检索）
   ├─ Data Node（数据写入）
   ├─ Index Node（构建索引）
   └─ Storage（MinIO/S3）
```

### 2. Collection 设计

#### 2.1 商品向量库（product_vectors）

```python
from pymilvus import Collection, FieldSchema, CollectionSchema, DataType

fields = [
    FieldSchema(name="product_id", dtype=DataType.INT64, is_primary=True),
    FieldSchema(name="title", dtype=DataType.VARCHAR, max_length=512),
    FieldSchema(name="description", dtype=DataType.VARCHAR, max_length=4096),
    FieldSchema(name="category_id", dtype=DataType.INT64),
    FieldSchema(name="price", dtype=DataType.DOUBLE),
    FieldSchema(name="embedding", dtype=DataType.FLOAT_VECTOR, dim=1024),
    FieldSchema(name="tags", dtype=DataType.ARRAY,
                element_type=DataType.VARCHAR, max_length=64, max_capacity=10),
    FieldSchema(name="status", dtype=DataType.INT32),
    FieldSchema(name="publish_time", dtype=DataType.INT64),
]

schema = CollectionSchema(fields, description="商品向量库")

collection = Collection("product_vectors", schema)

# 创建索引
index_params = {
    "metric_type": "COSINE",
    "index_type": "HNSW",
    "params": {"M": 16, "efConstruction": 200}
}
collection.create_index("embedding", index_params)
```

#### 2.2 FAQ 知识库（faq_vectors）

```python
fields = [
    FieldSchema(name="faq_id", dtype=DataType.INT64, is_primary=True),
    FieldSchema(name="question", dtype=DataType.VARCHAR, max_length=512),
    FieldSchema(name="answer", dtype=DataType.VARCHAR, max_length=2048),
    FieldSchema(name="category", dtype=DataType.VARCHAR, max_length=64),
    FieldSchema(name="embedding", dtype=DataType.FLOAT_VECTOR, dim=1024),
]
```

#### 2.3 商品描述爆款库（hot_description_vectors）

```python
fields = [
    FieldSchema(name="desc_id", dtype=DataType.INT64, is_primary=True),
    FieldSchema(name="category_id", dtype=DataType.INT64),
    FieldSchema(name="description", dtype=DataType.VARCHAR, max_length=2048),
    FieldSchema(name="view_count", dtype=DataType.INT64),
    FieldSchema(name="embedding", dtype=DataType.FLOAT_VECTOR, dim=1024),
]
```

### 3. 向量库集成代码

```java
@Configuration
public class MilvusConfig {

    @Bean
    public MilvusClient milvusClient() {
        ConnectParam connectParam = ConnectParam.builder()
            .uri("http://milvus:19530")
            .token("root:Milvus")
            .build();
        return new MilvusClient(connectParam);
    }
}

@Service
public class MilvusServiceImpl implements MilvusDubboService {

    @Resource
    private MilvusClient milvusClient;

    @Resource
    private ChatClient embeddingClient;

    /**
     * 写入商品向量
     */
    @Override
    public void upsertProductVector(ProductVector vector) {
        // 1. 生成 embedding
        float[] embedding = embeddingClient.embed(
            vector.getTitle() + " " + vector.getDescription());

        // 2. 构造插入数据
        List<JSONObject> rows = new ArrayList<>();
        JSONObject row = new JSONObject();
        row.put("product_id", vector.getProductId());
        row.put("title", vector.getTitle());
        row.put("description", vector.getDescription());
        row.put("category_id", vector.getCategoryId());
        row.put("price", vector.getPrice());
        row.put("embedding", embedding);
        rows.add(row);

        // 3. Upsert
        milvusClient.upsert(UpsertReq.builder()
            .collectionName("product_vectors")
            .data(rows)
            .build());
    }

    /**
     * 向量检索（语义搜索）
     */
    @Override
    public List<ProductSimilarVO> searchSimilar(String query, int topK) {
        // 1. 查询 embedding
        float[] queryEmbedding = embeddingClient.embed(query);

        // 2. 向量检索
        SearchResp resp = milvusClient.search(SearchReq.builder()
            .collectionName("product_vectors")
            .data(Collections.singletonList(queryEmbedding))
            .topK(topK)
            .filter("status == 3")  // 只检索在售
            .outputFields(Arrays.asList(
                "product_id", "title", "price", "category_id"))
            .build());

        // 3. 转换结果
        return resp.getResults().get(0).stream()
            .map(hit -> {
                ProductSimilarVO vo = new ProductSimilarVO();
                vo.setProductId((Long) hit.getEntity().get("product_id"));
                vo.setTitle((String) hit.getEntity().get("title"));
                vo.setPrice((Double) hit.getEntity().get("price"));
                vo.setScore(hit.getScore());
                return vo;
            })
            .collect(Collectors.toList());
    }
}
```

### 4. 向量库数据同步

```java
@Component
public class VectorSyncListener {

    @Resource
    private MilvusDubboService milvusService;

    @KafkaListener(topics = "product-events", groupId = "vector-sync-group")
    public void onProductEvent(ProductEvent event) {
        switch (event.getType()) {
            case CREATED:
            case UPDATED:
            case PUBLISHED:
                // 同步到向量库
                ProductVector vector = buildVector(event);
                milvusService.upsertProductVector(vector);
                break;

            case OFF_SHELF:
            case DELETED:
                // 从向量库删除
                milvusService.deleteProductVector(event.getProductId());
                break;
        }
    }
}
```

### 5. Embedding 模型选择

| 模型 | 维度 | 性能 | 适用 |
|---|---|---|---|
| MiniMax-M3-Embedding | 1024 | ⭐⭐⭐⭐⭐ | 生产首选 |
| BGE-large-zh-v1.5 | 1024 | ⭐⭐⭐⭐ | 开源备选 |
| M3E-large | 1024 | ⭐⭐⭐ | 开源备选 |

## 第二部分：Docker Compose 完整部署

### 1. 整体架构

```
                [Nginx 1.24] (反向代理 + 静态资源)
                       ↓
                [Spring Cloud Gateway]
                       ↓
        ┌──────────────┴──────────────┐
        ↓              ↓              ↓
  [14 个微服务]   [8 类中间件]   [3 类监控]
```

### 2. 完整 docker-compose.yml

```yaml
version: "3.8"

networks:
  zhihuan-network:
    driver: bridge

volumes:
  mysql-data:
  redis-data:
  es-data:
  kafka-data:
  minio-data:
  prometheus-data:
  grafana-data:

services:
  # ========== 基础组件 ==========
  nacos:
    image: nacos/nacos-server:v3.0.3
    container_name: zhihuan-nacos
    environment:
      - MODE=standalone
      - JVM_XMS=512m
      - JVM_XMX=512m
      - SPRING_DATASOURCE_PLATFORM=mysql
      - MYSQL_SERVICE_HOST=mysql
      - MYSQL_SERVICE_PORT=3306
      - NACOS_AUTH_ENABLE=true
    ports:
      - "8848:8848"
      - "9848:9848"
    networks:
      - zhihuan-network

  # ========== 数据库 ==========
  mysql:
    image: mysql:8.0
    container_name: zhihuan-mysql
    environment:
      - MYSQL_ROOT_PASSWORD=root123
      - MYSQL_USER=zhihuan
      - MYSQL_PASSWORD=zhihuan123
    command: >
      --character-set-server=utf8mb4
      --collation-server=utf8mb4_unicode_ci
      --default-authentication-plugin=mysql_native_password
      --max_connections=1000
    ports:
      - "3306:3306"
    volumes:
      - mysql-data:/var/lib/mysql
      - ./init-sql:/docker-entrypoint-initdb.d
    networks:
      - zhihuan-network

  # ========== 缓存 ==========
  redis:
    image: redis:7.4-alpine
    container_name: zhihuan-redis
    command: >
      redis-server
      --cluster-enabled yes
      --cluster-config-file nodes.conf
      --port 6379
      --requirepass zhihuan123
      --maxmemory 2gb
      --maxmemory-policy allkeys-lru
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data
    networks:
      - zhihuan-network

  # ========== 搜索引擎 ==========
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.15.0
    container_name: zhihuan-es
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
      - ES_JAVA_OPTS=-Xms1g -Xmx1g
    ports:
      - "9200:9200"
      - "9300:9300"
    volumes:
      - es-data:/usr/share/elasticsearch/data
    networks:
      - zhihuan-network

  # ========== 消息队列 ==========
  kafka:
    image: confluentinc/cp-kafka:7.6.1
    container_name: zhihuan-kafka
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_PROCESS_ROLES: "broker,controller"
      KAFKA_LISTENERS: "PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093"
      KAFKA_CONTROLLER_QUORUM_VOTERS: "1@kafka:9093"
      KAFKA_ADVERTISED_LISTENERS: "PLAINTEXT://kafka:9092"
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
    ports:
      - "9092:9092"
    volumes:
      - kafka-data:/var/lib/kafka/data
    networks:
      - zhihuan-network

  rocketmq:
    image: apache/rocketmq:5.3.1
    container_name: zhihuan-rocketmq
    ports:
      - "9876:9876"
      - "10911:10911"
    networks:
      - zhihuan-network

  # ========== 向量数据库 ==========
  milvus:
    image: milvusdb/milvus:v2.4.0
    container_name: zhihuan-milvus
    command: ["milvus", "run", "standalone"]
    environment:
      ETCD_ENDPOINTS: etcd:2379
      MINIO_ADDRESS: minio:9000
    ports:
      - "19530:19530"
      - "9091:9091"
    depends_on:
      - etcd
      - minio
    networks:
      - zhihuan-network

  etcd:
    image: quay.io/coreos/etcd:v3.5.5
    container_name: zhihuan-etcd
    command: etcd --advertise-client-urls=http://etcd:2379 --listen-client-urls http://0.0.0.0:2379 --data-dir /etcd
    networks:
      - zhihuan-network

  minio:
    image: minio/minio:RELEASE.2024-08-17T01-24-54Z
    container_name: zhihuan-minio
    command: minio server /data --console-address ":9001"
    environment:
      MINIO_ROOT_USER: minio
      MINIO_ROOT_PASSWORD: minio123
    ports:
      - "9000:9000"
      - "9001:9001"
    volumes:
      - minio-data:/data
    networks:
      - zhihuan-network

  # ========== 任务调度 ==========
  xxl-job-admin:
    image: xuxueli/xxl-job-admin:2.5.0
    container_name: zhihuan-xxl-job
    environment:
      PARAMS: >
        --spring.datasource.url=jdbc:mysql://mysql:3306/xxl_job
        --spring.datasource.username=zhihuan
        --spring.datasource.password=zhihuan123
    ports:
      - "8080:8080"
    depends_on:
      - mysql
    networks:
      - zhihuan-network

  # ========== 微服务 ==========
  zhihuan-gateway:
    build: ./zhihuan-gateway
    container_name: zhihuan-gateway
    ports:
      - "9000:9000"
    environment:
      - NACOS_SERVER=nacos:8848
    depends_on:
      - nacos
    networks:
      - zhihuan-network

  zhihuan-user:
    build: ./zhihuan-user
    ports:
      - "9011:9011"
    environment:
      - NACOS_SERVER=nacos:8848
    networks:
      - zhihuan-network

  zhihuan-product:
    build: ./zhihuan-product
    ports:
      - "9012:9012"
    networks:
      - zhihuan-network

  zhihuan-ai:
    build: ./zhihuan-ai
    ports:
      - "9013:9013"
    environment:
      - DASHSCOPE_API_KEY=${DASHSCOPE_API_KEY}
    networks:
      - zhihuan-network

  zhihuan-trade:
    build: ./zhihuan-trade
    ports:
      - "9014:9014"
    networks:
      - zhihuan-network

  # ... 其他服务类似

  # ========== 监控 ==========
  prometheus:
    image: prom/prometheus:v2.48.0
    container_name: zhihuan-prometheus
    volumes:
      - ./prometheus/prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus-data:/prometheus
    ports:
      - "9090:9090"
    networks:
      - zhihuan-network

  grafana:
    image: grafana/grafana:10.2.0
    container_name: zhihuan-grafana
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
    volumes:
      - grafana-data:/var/lib/grafana
    ports:
      - "3000:3000"
    networks:
      - zhihuan-network

  kibana:
    image: docker.elastic.co/kibana/kibana:8.15.0
    container_name: zhihuan-kibana
    ports:
      - "5601:5601"
    depends_on:
      - elasticsearch
    networks:
      - zhihuan-network

  skywalking-oap:
    image: apache/skywalking-oap-server:9.7.0
    container_name: zhihuan-skywalking
    ports:
      - "11800:11800"
      - "12800:12800"
    networks:
      - zhihuan-network

  skywalking-ui:
    image: apache/skywalking-ui:9.7.0
    container_name: zhihuan-skywalking-ui
    ports:
      - "8081:8080"
    depends_on:
      - skywalking-oap
    networks:
      - zhihuan-network

  # ========== 反向代理 ==========
  nginx:
    image: nginx:1.24-alpine
    container_name: zhihuan-nginx
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf
      - ./nginx/conf.d:/etc/nginx/conf.d
    ports:
      - "80:80"
      - "443:443"
    depends_on:
      - zhihuan-gateway
    networks:
      - zhihuan-network
```

### 3. Nginx 配置

```nginx
# /etc/nginx/nginx.conf
user nginx;
worker_processes auto;
error_log /var/log/nginx/error.log warn;
pid /var/run/nginx.pid;

events {
    worker_connections 4096;
    use epoll;
    multi_accept on;
}

http {
    include       /etc/nginx/mime.types;
    default_type  application/octet-stream;

    log_format main '$remote_addr - $remote_user [$time_local] "$request" '
                    '$status $body_bytes_sent "$http_referer" '
                    '"$http_user_agent" "$http_x_forwarded_for"';

    access_log /var/log/nginx/access.log main;

    sendgfile on;
    tcp_nopush on;
    tcp_nodelay on;
    keepalive_timeout 65;
    types_hash_max_size 2048;

    # 启用 gzip
    gzip on;
    gzip_min_length 1k;
    gzip_comp_level 6;
    gzip_types text/plain text/css text/javascript application/json application/javascript;

    # 代理缓存配置
    proxy_cache_path /var/cache/nginx/proxy_cache levels=1:2 keys_zone=zhihuan_cache:100m max_size=10g inactive=60m use_temp_path=off;

    include /etc/nginx/conf.d/*.conf;
}
```

```nginx
# /etc/nginx/conf.d/zhihuan.conf
upstream zhihuan_gateway {
    server zhihuan-gateway:9000 max_fails=3 fail_timeout=30s;
    keepalive 32;
}

server {
    listen 80;
    server_name zhihuan.local;

    # 静态资源（直接走 Nginx）
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf)$ {
        proxy_cache zhihuan_cache;
        proxy_cache_valid 200 7d;
        proxy_cache_valid 404 1m;
        expires 7d;
        access_log off;
        try_files $uri @backend;
    }

    # API 请求 → Gateway
    location /api/ {
        proxy_pass http://zhihuan_gateway;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        proxy_connect_timeout 5s;
        proxy_send_timeout 30s;
        proxy_read_timeout 30s;

        proxy_buffering on;
        proxy_buffer_size 16k;
        proxy_buffers 4 32k;

        # 限流
        limit_req zone=api_limit burst=20 nodelay;
    }

    # SSE / WebSocket
    location /api/im/ws {
        proxy_pass http://zhihuan_gateway;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_read_timeout 86400s;
    }

    location @backend {
        proxy_pass http://zhihuan_gateway;
    }
}

# 限流配置
limit_req_zone $binary_remote_addr zone=api_limit:10m rate=100r/s;
```

### 4. 一键启动脚本

```bash
#!/bin/bash
# start.sh

set -e

echo "===================="
echo "启动智换项目"
echo "===================="

# 1. 启动中间件
echo "[1/3] 启动中间件 (MySQL/Redis/Kafka/ES/Milvus/...)"
docker-compose up -d mysql redis nacos kafka elasticsearch \
                     milvus etcd minio rocketmq xxl-job-admin

# 等待中间件启动
echo "[2/3] 等待中间件启动完成..."
sleep 30

# 2. 初始化数据库
echo "[3/3] 初始化数据库..."
docker exec zhihuan-mysql mysql -uroot -proot123 -e "
  CREATE DATABASE IF NOT EXISTS zhihuan_user DEFAULT CHARACTER SET utf8mb4;
  CREATE DATABASE IF NOT EXISTS zhihuan_product DEFAULT CHARACTER SET utf8mb4;
  -- ... 其他数据库
"

# 3. 启动微服务
echo "[4/4] 启动微服务..."
docker-compose up -d zhihuan-gateway zhihuan-user zhihuan-product \
                     zhihuan-ai zhihuan-trade zhihuan-search \
                     zhihuan-recommend zhihuan-feed zhihuan-marketing \
                     zhihuan-im zhihuan-audit zhihuan-notification \
                     zhihuan-job zhihuan-admin

# 4. 启动监控
docker-compose up -d prometheus grafana kibana skywalking-oap skywalking-ui nginx

echo "===================="
echo "启动完成！"
echo "===================="
echo "Nginx:        http://localhost"
echo "Gateway:      http://localhost:9000"
echo "Nacos:        http://localhost:8848/nacos (nacos/nacos)"
echo "Grafana:      http://localhost:3000 (admin/admin)"
echo "Kibana:       http://localhost:5601"
echo "SkyWalking:   http://localhost:8081"
echo "MinIO:        http://localhost:9001 (minio/minio123)"
echo "XXL-JOB:      http://localhost:8080/xxl-job-admin (admin/123456)"
echo "===================="
```

## 4. 简历话术

> 设计 Docker Compose 一键部署方案：包含 14 个微服务 + 8 类中间件 + 3 类监控组件，本地一条命令即可启动完整开发环境；Nginx 配置反向代理 + 静态资源 + WebSocket 支持 + 限流；编写一键启动脚本（start.sh）自动初始化数据库 + 启动服务 + 输出所有访问地址，新成员上手时间从 1 天缩短到 10 分钟。
