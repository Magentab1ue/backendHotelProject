package com.example.lovelypet.service;

import com.example.lovelypet.entity.PhotoRoom;
import com.example.lovelypet.entity.Room;
import com.example.lovelypet.exception.BaseException;
import com.example.lovelypet.exception.PhotoRoomException;
import com.example.lovelypet.repository.PhotoRoomRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class PhotoRoomService {

    private final PhotoRoomRepository photoRoomRepository;


    public PhotoRoomService(PhotoRoomRepository photoRoomRepository) {
        this.photoRoomRepository = photoRoomRepository;

    }

    public List<PhotoRoom> findById(Room id) {
        return  photoRoomRepository.findAll();
    }

    public List<PhotoRoom> findAll() {
        return  photoRoomRepository.findAll();
    }

    public PhotoRoom create(
            String partFile,
            Room roomId
    ) throws BaseException {

        //validate
        if (Objects.isNull(partFile)) {
            throw PhotoRoomException.createPartFileNull();
        }

        if (Objects.isNull(roomId)) {
            throw PhotoRoomException.createRoomIdNull();
        }

        //verify


        PhotoRoom entity = new PhotoRoom();
        entity.setPhotoRoomPartFile(partFile);
        entity.setRoomId(roomId);
        return photoRoomRepository.save(entity);
    }

}
