package com.example.lovelypet.business;

import com.example.lovelypet.entity.PhotoRoom;
import com.example.lovelypet.entity.Room;
import com.example.lovelypet.exception.BaseException;
import com.example.lovelypet.exception.PhotoRoomException;
import com.example.lovelypet.exception.RoomException;
import com.example.lovelypet.service.PhotoRoomService;
import com.example.lovelypet.service.RoomService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class PhotoRoomBusiness {

    private final PhotoRoomService photoRoomService;

    private final RoomService roomService;

    public PhotoRoomBusiness(PhotoRoomService photoRoomService, RoomService roomService) {
        this.photoRoomService = photoRoomService;
        this.roomService = roomService;
    }

    public PhotoRoom uploadImage(MultipartFile file, int id) throws IOException, BaseException {

        if (file.isEmpty()) {
            throw PhotoRoomException.createPartFileNull();
        }

        if (Objects.isNull(id)) {
            throw RoomException.createRoomIdNull();
        }

        Optional<Room> optIdRoom = roomService.findById(id);
        Room idRoom = optIdRoom.get();

        // สร้างชื่อไฟล์ที่ไม่ซ้ำกัน
        String fileName = generateUniqueFileName(file.getOriginalFilename());

        String uploadDir = "./uploaded_images"; // เปลี่ยนตามต้องการให้เหมาะสมกับโฟลเดอร์ที่ต้องการบันทึกรูปภาพ
        String filePath = uploadDir + File.separator + fileName;

        // สร้างไดเร็กทอรีถ้ายังไม่มี
        File directory = new File(uploadDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // Save the image file
        Path path = Paths.get(filePath);
        Files.write(path, file.getBytes());

        // Save the image information in the database
        PhotoRoom response = photoRoomService.create(fileName, idRoom);

        return response;
    }

    // สร้างชื่อไฟล์ที่ไม่ซ้ำกัน
    private String generateUniqueFileName(String originalFileName) {
        return UUID.randomUUID().toString() + "_" + originalFileName;
    }
}


