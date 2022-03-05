import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.nio.conn.ssl.SSLIOSessionStrategy;
import org.apache.http.ssl.SSLContextBuilder;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetRequest;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Es7Test {

    private static String hostname = "127.0.0.1";
    private static Integer port = 9200;
    private static String scheme = "https";
    public static String username = "elastic";
    public static String password = "EaPvk=T+6N5j3Wy+QZVR";

    public static void main(String[] args) {
//        index();
//        bulk();
//        get();
        multiGet();
    }

    /**
     * 插入一条数据（如果存在则更新）
     * http://localhost:9200/user/_doc/1
     */
    public static void index() {
        RestHighLevelClient client = getClient();

        Map<String, Object> jsonMap = new HashMap<>();
        jsonMap.put("user", "richard1");
        jsonMap.put("postDate", new Date());
        jsonMap.put("message", "trying out Elasticsearch again");
        IndexRequest request = new IndexRequest("user").id("2").source(jsonMap);

        IndexResponse response = null;
        try {
            response = client.index(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String index = response.getIndex();
        String id = response.getId();
        System.out.println(index + "---" + id);

        close(client);
    }

    /**
     * 获取一个文档
     */
    public static void get() {
        RestHighLevelClient client = getClient();
        GetRequest getRequest = new GetRequest("user", "1");
        try {
            GetResponse getResponse = client.get(getRequest, RequestOptions.DEFAULT);
            String index = getResponse.getIndex();
            String id = getResponse.getId();
            if (getResponse.isExists()) {
                long version = getResponse.getVersion();
                String sourceAsString = getResponse.getSourceAsString();
                Map<String, Object> sourceAsMap = getResponse.getSourceAsMap();
                byte[] sourceAsBytes = getResponse.getSourceAsBytes();

                System.out.println("索引：" + index + "，ID：" + id + "版本：" + version);
                System.out.println(sourceAsString);
                System.out.println(sourceAsMap);
                System.out.println(sourceAsBytes.toString());
            } else {
                System.out.println("未查到结果");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        close(client);
    }

    /**
     * 文档是否存在
     */
    public static void exists() {
        RestHighLevelClient client = getClient();
        GetRequest getRequest = new GetRequest("user", "1");
        // 禁止获取_source
        getRequest.fetchSourceContext(new FetchSourceContext(false));
        // 禁止获取存储字段
        getRequest.storedFields("_none_");
        try {
            boolean exists = client.exists(getRequest, RequestOptions.DEFAULT);
            System.out.println(getRequest.index() + "---" + getRequest.id() + "---文档是否存在：" + exists);
        } catch (IOException e) {
            // TODO 自动生成的 catch 块
            e.printStackTrace();
        }
        close(client);
    }

    /**
     * 删除文档
     */
    public static void delete() {
        RestHighLevelClient client = getClient();
        DeleteRequest deleteRequest = new DeleteRequest("user", "1");
        try {
            client.delete(deleteRequest, RequestOptions.DEFAULT);
            System.out.println(deleteRequest.index() + "---" + deleteRequest.id() + ": 文档已删除");
            exists();
        } catch (IOException e) {
            e.printStackTrace();
        }
        close(client);
    }

    public static void update() {
        RestHighLevelClient client = getClient();
        Map<String, Object> jsonMap = new HashMap<>();
        jsonMap.put("updated", new Date());
        jsonMap.put("reason", "daily update");
        UpdateRequest request = new UpdateRequest("user", "2").doc(jsonMap);
        try {
            UpdateResponse response = client.update(request, RequestOptions.DEFAULT);
            System.out.println(response.getIndex() + "---" + response.getId() + "完成更新");
        } catch (IOException e) {
            e.printStackTrace();
        }
        close(client);
    }

    /**
     * 批量更新
     */
    public static void bulk() {
        RestHighLevelClient client = getClient();

        BulkRequest request = new BulkRequest();
        request.add(new IndexRequest("user").id("3").source(XContentType.JSON, "field", "foo", "user", "lucky"));
        request.add(new IndexRequest("user").id("4").source(XContentType.JSON, "field", "bar", "user", "Jon"));
        request.add(new IndexRequest("user").id("5").source(XContentType.JSON, "field", "baz", "user", "Lucy"));
        // id为10的不存在
        request.add(new DeleteRequest("user", "7"));
        request.add(new UpdateRequest("user", "8").doc(XContentType.JSON, "other", "test"));
        BulkResponse bulkResponse = null;
        try {
            bulkResponse = client.bulk(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 获取执行状态
        System.out.println("批量更新结果状态：" + bulkResponse.status());
        close(client);
    }

    // 批量查询
    public static void multiGet() {
        RestHighLevelClient client = getSecureClient();

        MultiGetRequest request = new MultiGetRequest();
        request.add(new MultiGetRequest.Item("user", "2"));
        request.add(new MultiGetRequest.Item("user", "4"));
        request.add(new MultiGetRequest.Item("user", "5"));

        MultiGetResponse mget = null;
        try {
            mget = client.mget(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 打印查詢結果
        System.out.println("mget:");
        mget.forEach(item -> System.out.println(item.getResponse().getSourceAsString()));
        close(client);
    }

    /**
     * 异步操作.
     */
    public static void async() {

        ActionListener<SearchResponse> listener = new ActionListener<SearchResponse>() {
            @Override
            public void onResponse(SearchResponse searchResponse) {
                // 查询成功
            }

            @Override
            public void onFailure(Exception e) {
                // 查询失败
            }
        };
        SearchRequest request = new SearchRequest("user");
        getClient().searchAsync(request, RequestOptions.DEFAULT, listener);
    }

    /**
     * 获取客户端 for http
     *
     * @return
     */
    private static RestHighLevelClient getClient() {
        RestHighLevelClient client = new RestHighLevelClient(
            RestClient.builder(
                new HttpHost(hostname, port, scheme)
            )
        );
        return client;
    }

    private static RestHighLevelClient getSecureClient() {
        System.out.println("Elasticsearch init start ......");
        RestHighLevelClient restClient = null;
        try {
            final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(username, password));

            SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
                // 信任所有
                public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    return true;
                }
            }).build();
            SSLIOSessionStrategy sessionStrategy = new SSLIOSessionStrategy(sslContext, NoopHostnameVerifier.INSTANCE);
            restClient = new RestHighLevelClient(
                RestClient.builder(
                    new HttpHost(hostname, port, scheme))
                    .setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
                        public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
                            httpClientBuilder.disableAuthCaching();
                            httpClientBuilder.setSSLStrategy(sessionStrategy);
                            httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                            return httpClientBuilder;
                        }
                    }));
        } catch (Exception e) {
            System.err.println(e);
        }
        return restClient;
    }

    /**
     * 关闭客户端
     *
     * @param client
     */
    private static void close(RestHighLevelClient client) {
        try {
            client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
