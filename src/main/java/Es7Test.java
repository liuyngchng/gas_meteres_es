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
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Es7Test {

    private static String hostName = "127.0.0.1";
    private static Integer port = 9200;
    private static String scheme = "https";
    private static String userName = "elastic";
    private static String password = "EaPvk=T+6N5j3Wy+QZVR";
    private static String indexName = "user";

    public static void main(String[] args) {
//        index();
//        bulk();
//        get();
//        multiGet();
        basicSearch();
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
        IndexRequest request = new IndexRequest(indexName).id("2").source(jsonMap);

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
        GetRequest getRequest = new GetRequest(indexName, "1");
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
        GetRequest getRequest = new GetRequest(indexName, "1");
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
        DeleteRequest deleteRequest = new DeleteRequest(indexName, "1");
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
        UpdateRequest request = new UpdateRequest(indexName, "2").doc(jsonMap);
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
        request.add(new IndexRequest(indexName).id("3").source(XContentType.JSON, "field", "foo", "user", "lucky"));
        request.add(new IndexRequest(indexName).id("4").source(XContentType.JSON, "field", "bar", "user", "Jon"));
        request.add(new IndexRequest(indexName).id("5").source(XContentType.JSON, "field", "baz", "user", "Lucy"));
        // id为10的不存在
        request.add(new DeleteRequest(indexName, "7"));
        request.add(new UpdateRequest(indexName, "8").doc(XContentType.JSON, "other", "test"));
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
        request.add(new MultiGetRequest.Item(indexName, "2"));
        request.add(new MultiGetRequest.Item(indexName, "4"));
        request.add(new MultiGetRequest.Item(indexName, "5"));

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
        SearchRequest request = new SearchRequest(indexName);
        getClient().searchAsync(request, RequestOptions.DEFAULT, listener);
    }

    /**
    GET shop-trade/doc/_search
    {
        "query": {
            "bool": {
                "must": [
                { "match":{ "spu_name": "小米" } },
                { "match": { "status": "1" } }
            ]
            }
        }
    }
     https://www.cnblogs.com/slowcity/p/11727579.html
    **/
    public static void search() {
        try {
//            BoolQueryBuilder boolBuilder = QueryBuilders.boolQuery();
//            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
//            //这里可以根据字段进行搜索，must表示符合条件的，相反的must not表示不符合条件的
//            MatchQueryBuilder matchQueryBuilder = QueryBuilders.matchQuery("user", "richard");
//            // RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery("fields_timestamp"); //新建range条件
//            // rangeQueryBuilder.gte("2019-03-21T08:24:37.873Z"); //开始时间
//            // rangeQueryBuilder.lte("2019-03-21T08:24:37.873Z"); //结束时间
//            // boolBuilder.must(rangeQueryBuilder);
//            boolBuilder.must(matchQueryBuilder);
//            sourceBuilder.query(boolBuilder);           //设置查询，可以是任何类型的QueryBuilder。
//            sourceBuilder.from(0);                      //设置确定结果要从哪个索引开始搜索的from选项，默认为0
//            sourceBuilder.size(100);                    //设置确定搜素命中返回数的size选项，默认为10
//            sourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS)); //设置一个可选的超时，控制允许搜索的时间。
//            //第一个是获取字段，第二个是过滤的字段，默认获取全部
//            sourceBuilder.fetchSource(new String[] {"user","message","postDate"}, new String[] {});
//            SearchRequest searchRequest = new SearchRequest(indexName); //索引
//            searchRequest.types("_doc");                 //类型
//            searchRequest.source(sourceBuilder);
//            SearchResponse response = getSecureClient().search(searchRequest, RequestOptions.DEFAULT);
//            SearchHits hits = response.getHits();       //SearchHits提供有关所有匹配的全局信息，例如总命中数或最高分数：
//            SearchHit[] searchHits = hits.getHits();
//            for (SearchHit hit : searchHits) {
//                System.out.println("search -> {}");
//                System.out.println(hit.getSourceAsString());
//            }
//            String result = Arrays.toString(searchHits);
//            System.out.println(result);
        } catch (Exception ex) {
            System.out.println(ex);
        }
    }

    /**
     * basic search
     */
    public static void basicSearch() {
        try {
            // 基础设置
            SearchRequest searchRequest = new SearchRequest(indexName);
            // 搜索源构建对象
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            // 匹配关键字
            searchSourceBuilder.query(
                QueryBuilders.matchQuery("user", "richard").minimumShouldMatch("1%"));   //minimumShouldMatch 最小匹配度
            searchRequest.source(searchSourceBuilder);
            // 发起请求，获取结果
            SearchResponse searchResponse = getSecureClient().search(searchRequest, RequestOptions.DEFAULT);
            //注意默认返回10条数据  解决办法可以用分页查询，具体实现 下面第一快代码有
            SearchHits hits = searchResponse.getHits();
            SearchHit[] searchHits = hits.getHits();
            for (SearchHit hit : searchHits) {
                System.out.println("search -> {}");
                System.out.println(hit.getSourceAsString());
            }
            String result = Arrays.toString(searchHits);
            System.out.println(result);
        } catch (Exception ex) {
            System.out.println(ex);
        }

    }



    /**
     * 获取客户端 for http
     *
     * @return
     */
    private static RestHighLevelClient getClient() {
        RestHighLevelClient client = new RestHighLevelClient(
            RestClient.builder(
                new HttpHost(hostName, port, scheme)
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
                new UsernamePasswordCredentials(userName, password));

            SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
                // 信任所有
                public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    return true;
                }
            }).build();
            SSLIOSessionStrategy sessionStrategy = new SSLIOSessionStrategy(sslContext, NoopHostnameVerifier.INSTANCE);
            restClient = new RestHighLevelClient(
                RestClient.builder(
                    new HttpHost(hostName, port, scheme))
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
