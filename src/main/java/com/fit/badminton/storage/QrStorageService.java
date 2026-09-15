package com.fit.badminton.storage; public interface QrStorageService {String upload(long memberId,byte[] bytes,String contentType); QrPayload load(String storagePath);}
