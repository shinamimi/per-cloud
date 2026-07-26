package com.cloud.backend.constant;

public interface FileConstants {

    long KB = 1024;
    long MB = KB * 1024;
    long GB = MB * 1024;

    long DEFAULT_QUOTA = 10 * GB;
    long DEFAULT_CHUNK_SIZE = 10 * MB;
    long ROOT_PARENT_ID = 0;

    int IMAGE = 0;
    int DOCUMENT = 1;
    int VIDEO = 2;
    int AUDIO = 3;
    int ARCHIVE = 4;
    int OTHER = 5;
}