(ns metabase.driver.dynamodb.util
  (:import [com.amazonaws.services.dynamodbv2 AmazonDynamoDBClientBuilder AmazonDynamoDB]
           [com.amazonaws.client.builder AwsClientBuilder AwsClientBuilder$EndpointConfiguration]
           [com.amazonaws.auth AWSStaticCredentialsProvider BasicAWSCredentials]

           ))

(def ^:dynamic ^AmazonDynamoDB *dynamodb-client* nil)

(defn -with-dynamodb-client
  [f database]
  (let [builder (AmazonDynamoDBClientBuilder/standard)
        region-id         (get-in database [:details :region-id])
        access-key-id     (get-in database [:details :access-key-id])
        secret-access-key (get-in database [:details :secret-access-key])]

    (if-let [endpoint (get-in database [:details :endpoint])]
      (.withEndpointConfiguration builder (AwsClientBuilder$EndpointConfiguration. endpoint region-id))
      (when region-id
        (.withRegion builder region-id)))

    (when (and access-key-id secret-access-key)
      (.withCredentials builder (AWSStaticCredentialsProvider. (BasicAWSCredentials. access-key-id secret-access-key))))

    (when-let [client (.build builder)]
      (try
        (binding [*dynamodb-client* client]
          (f *dynamodb-client*))
        (finally (.shutdown client))))))

(defmacro with-dynamodb-client
  "Open a new DynamoDB client"
  [[binding database] & body]
  `(let [f# (fn [~binding]
             ~@body)]
    (if *dynamodb-client*
      (f# *dynamodb-client*)
      (-with-dynamodb-client f# ~database))))
