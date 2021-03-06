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
//    password for mac-mini
//    private static String password = "EaPvk=T+6N5j3Wy+QZVR";
//    password for ubuntu20.04 in T14
    private static String password = "q+jy_9qzovxwajQRoVRe";
    private static String indexName = "user";

    public static void main(String[] args) {
//        index();
//        bulk();
//        get();
//        multiGet();
//        basicSearch();
        multiMatchSearch();
    }

    /**
     * ?????????????????????????????????????????????
     * http://localhost:9200/user/_doc/1
     */
    public static void index() {
        RestHighLevelClient client = getSecureClient();

        Map<String, Object> jsonMap = new HashMap<>();
        jsonMap.put("user", "richard");
        jsonMap.put("postDate", new Date());
        jsonMap.put("message", "trying out Elasticsearch");
        IndexRequest request = new IndexRequest(indexName).id("1").source(jsonMap);

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
     * ??????????????????
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

                System.out.println("?????????" + index + "???ID???" + id + "?????????" + version);
                System.out.println(sourceAsString);
                System.out.println(sourceAsMap);
                System.out.println(sourceAsBytes.toString());
            } else {
                System.out.println("???????????????");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        close(client);
    }

    /**
     * ??????????????????
     */
    public static void exists() {
        RestHighLevelClient client = getClient();
        GetRequest getRequest = new GetRequest(indexName, "1");
        // ????????????_source
        getRequest.fetchSourceContext(new FetchSourceContext(false));
        // ????????????????????????
        getRequest.storedFields("_none_");
        try {
            boolean exists = client.exists(getRequest, RequestOptions.DEFAULT);
            System.out.println(getRequest.index() + "---" + getRequest.id() + "---?????????????????????" + exists);
        } catch (IOException e) {
            // TODO ??????????????? catch ???
            e.printStackTrace();
        }
        close(client);
    }

    /**
     * ????????????
     */
    public static void delete() {
        RestHighLevelClient client = getClient();
        DeleteRequest deleteRequest = new DeleteRequest(indexName, "1");
        try {
            client.delete(deleteRequest, RequestOptions.DEFAULT);
            System.out.println(deleteRequest.index() + "---" + deleteRequest.id() + ": ???????????????");
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
            System.out.println(response.getIndex() + "---" + response.getId() + "????????????");
        } catch (IOException e) {
            e.printStackTrace();
        }
        close(client);
    }

    /**
     * ????????????
     */
    public static void bulk() {
        RestHighLevelClient client = getClient();

        BulkRequest request = new BulkRequest();
        request.add(new IndexRequest(indexName).id("3").source(XContentType.JSON, "field", "foo", "user", "lucky"));
        request.add(new IndexRequest(indexName).id("4").source(XContentType.JSON, "field", "bar", "user", "Jon"));
        request.add(new IndexRequest(indexName).id("5").source(XContentType.JSON, "field", "baz", "user", "Lucy"));
        // id???10????????????
        request.add(new DeleteRequest(indexName, "7"));
        request.add(new UpdateRequest(indexName, "8").doc(XContentType.JSON, "other", "test"));
        BulkResponse bulkResponse = null;
        try {
            bulkResponse = client.bulk(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // ??????????????????
        System.out.println("???????????????????????????" + bulkResponse.status());
        close(client);
    }

    // ????????????
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
        // ??????????????????
        System.out.println("mget:");
        mget.forEach(item -> System.out.println(item.getResponse().getSourceAsString()));
        close(client);
    }

    /**
     * ????????????.
     */
    public static void async() {

        ActionListener<SearchResponse> listener = new ActionListener<SearchResponse>() {
            @Override
            public void onResponse(SearchResponse searchResponse) {
                // ????????????
            }

            @Override
            public void onFailure(Exception e) {
                // ????????????
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
                { "match":{ "spu_name": "??????" } },
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
//            //???????????????????????????????????????must?????????????????????????????????must not????????????????????????
//            MatchQueryBuilder matchQueryBuilder = QueryBuilders.matchQuery("user", "richard");
//            // RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery("fields_timestamp"); //??????range??????
//            // rangeQueryBuilder.gte("2019-03-21T08:24:37.873Z"); //????????????
//            // rangeQueryBuilder.lte("2019-03-21T08:24:37.873Z"); //????????????
//            // boolBuilder.must(rangeQueryBuilder);
//            boolBuilder.must(matchQueryBuilder);
//            sourceBuilder.query(boolBuilder);           //???????????????????????????????????????QueryBuilder???
//            sourceBuilder.from(0);                      //???????????????????????????????????????????????????from??????????????????0
//            sourceBuilder.size(100);                    //????????????????????????????????????size??????????????????10
//            sourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS)); //????????????????????????????????????????????????????????????
//            //???????????????????????????????????????????????????????????????????????????
//            sourceBuilder.fetchSource(new String[] {"user","message","postDate"}, new String[] {});
//            SearchRequest searchRequest = new SearchRequest(indexName); //??????
//            searchRequest.types("_doc");                 //??????
//            searchRequest.source(sourceBuilder);
//            SearchResponse response = getSecureClient().search(searchRequest, RequestOptions.DEFAULT);
//            SearchHits hits = response.getHits();       //SearchHits??????????????????????????????????????????????????????????????????????????????
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
            // ????????????
            SearchRequest searchRequest = new SearchRequest(indexName);
            // ?????????????????????
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            // ???????????????
            searchSourceBuilder.query(
                QueryBuilders.matchQuery("user", "richard").minimumShouldMatch("1%"));   //minimumShouldMatch ???????????????
            searchRequest.source(searchSourceBuilder);
            // ???????????????????????????
            SearchResponse searchResponse = getSecureClient().search(searchRequest, RequestOptions.DEFAULT);
            //??????????????????10?????????  ???????????????????????????????????????????????? ????????????????????????
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
     * ?????????????????????????????????
     * @throws IOException
     */
    public static void multiMatchSearch() {
        try {
            // ????????????
            SearchRequest searchRequest = new SearchRequest(indexName);
            // ?????????????????????
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

            //????????????????????? toLowerCase() ?????????????????????????????????????????????????????????
            // ?????? searchSourceBuilder ????????????10 ??????????????????????????????????????????????????????
            BoolQueryBuilder queryBuilder =
                QueryBuilders.boolQuery().should(QueryBuilders.wildcardQuery("user",
                    ("*richard*").toLowerCase()));

            // ???????????????????????? ???????????? searchSourceBuilder.from(1);??????????????????????????????????????????MySQL??????limit
            searchSourceBuilder.from(0);  //?????????????????????????????????
            searchSourceBuilder.size(10);//????????????
            searchSourceBuilder.query(queryBuilder);

            searchRequest.source(searchSourceBuilder);
            // ???????????????????????????
            SearchResponse searchResponse;
            searchResponse = getSecureClient().search(searchRequest, RequestOptions.DEFAULT);
            SearchHits hits = searchResponse.getHits();
            // ???????????????????????????
            SearchHit[] searchHits = hits.getHits();
            // ???????????????
            System.out.println(searchHits.length);
            for (SearchHit searchHit : searchHits) {
                System.out.println(searchHit.toString());
                System.out.println("=============1");
            }
        } catch (Exception ex) {
            System.out.println(ex);
        }
    }


    /**
     * ??????????????? for http
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
                // ????????????
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
     * ???????????????
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
