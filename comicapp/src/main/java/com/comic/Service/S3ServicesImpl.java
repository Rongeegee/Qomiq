package com.comic.Service;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.comic.Utility.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class S3ServicesImpl implements S3Services {

    private Logger logger = LoggerFactory.getLogger(S3ServicesImpl.class);

    @Autowired
    private AmazonS3 s3client;

    @Value("${jsa.s3.bucket}")
    private String bucketName;

    @Override
    public S3Object downloadFile(String keyName) {
//        try {
            S3Object s3object = s3client.getObject(new GetObjectRequest(bucketName, keyName));
            s3object.getObjectContent();

//            ByteArrayOutputStream baos = new ByteArrayOutputStream();
//            int len;
//            byte[] buffer = new byte[4096];
//            while ((len = is.read(buffer, 0, buffer.length)) != -1) {
//                baos.write(buffer, 0, len);
//            }

            return s3object;
//        } catch (IOException ioe) {
//            logger.error("IOException: " + ioe.getMessage());
//        } catch (AmazonServiceException ase) {
//            logger.info("sCaught an AmazonServiceException from GET requests, rejected reasons:");
//            logger.info("Error Message:    " + ase.getMessage());
//            logger.info("HTTP Status Code: " + ase.getStatusCode());
//            logger.info("AWS Error Code:   " + ase.getErrorCode());
//            logger.info("Error Type:       " + ase.getErrorType());
//            logger.info("Request ID:       " + ase.getRequestId());
//            throw ase;
//        } catch (AmazonClientException ace) {
//            logger.info("Caught an AmazonClientException: ");
//            logger.info("Error Message: " + ace.getMessage());
//            throw ace;
//        }
//
//        return null;
    }

    @Override
    public void uploadFile(String keyName, S3Object s3Object) {
//        try {

            s3client.putObject(new PutObjectRequest(bucketName,keyName,s3Object.getObjectContent(),s3Object.getObjectMetadata()));
//        } catch(IOException ioe) {
//            logger.error("IOException: " + ioe.getMessage());
//        } catch (AmazonServiceException ase) {
//            logger.info("Caught an AmazonServiceException from PUT requests, rejected reasons:");
//            logger.info("Error Message:    " + ase.getMessage());
//            logger.info("HTTP Status Code: " + ase.getStatusCode());
//            logger.info("AWS Error Code:   " + ase.getErrorCode());
//            logger.info("Error Type:       " + ase.getErrorType());
//            logger.info("Request ID:       " + ase.getRequestId());
//            throw ase;
//        } catch (AmazonClientException ace) {
//            logger.info("Caught an AmazonClientException: ");
//            logger.info("Error Message: " + ace.getMessage());
//            throw ace;
//        }
    }

    @Override
    public void uploadFile(String keyName, MultipartFile file) {
        try {

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            s3client.putObject(bucketName, keyName, file.getInputStream(), metadata);
        } catch(IOException ioe) {
            logger.error("IOException: " + ioe.getMessage());
        } catch (AmazonServiceException ase) {
            logger.info("Caught an AmazonServiceException from PUT requests, rejected reasons:");
            logger.info("Error Message:    " + ase.getMessage());
            logger.info("HTTP Status Code: " + ase.getStatusCode());
            logger.info("AWS Error Code:   " + ase.getErrorCode());
            logger.info("Error Type:       " + ase.getErrorType());
            logger.info("Request ID:       " + ase.getRequestId());
            throw ase;
        } catch (AmazonClientException ace) {
            logger.info("Caught an AmazonClientException: ");
            logger.info("Error Message: " + ace.getMessage());
            throw ace;
        }
    }

    @Override
    public void deleteFileFromS3Bucket(String fileName){
       try {
           s3client.deleteObject(new DeleteObjectRequest(bucketName, fileName));
           }
       catch (AmazonServiceException ex) {
               logger.error("error [" + ex.getMessage() + "] occurred while removing [" + fileName + "] ");
            }
    }
    public List<String> listFiles() {

        ListObjectsRequest listObjectsRequest =
                new ListObjectsRequest()
                        .withBucketName(bucketName);
        //.withPrefix("test" + "/");

        List<String> keys = new ArrayList<>();

        ObjectListing objects = s3client.listObjects(listObjectsRequest);

        while (true) {
            List<S3ObjectSummary> summaries = objects.getObjectSummaries();
            if (summaries.size() < 1) {
                break;
            }

            for (S3ObjectSummary item : summaries) {
                if (!item.getKey().endsWith("/"))
                    keys.add(item.getKey());
            }

            objects = s3client.listNextBatchOfObjects(objects);
        }

        return keys;
    }

}
