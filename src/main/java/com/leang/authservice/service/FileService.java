package com.leang.authservice.service;

import com.leang.authservice.model.entity.FileMetaData;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public interface FileService {
	FileMetaData saveFile(MultipartFile file);

	InputStream getFileByFileName(String fileName);
}
