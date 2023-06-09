package com.example.lovelypet.repository;

import com.example.lovelypet.entity.Hotel;
import com.example.lovelypet.entity.Room;
import com.example.lovelypet.entity.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface RoomRepository extends CrudRepository<Room, String> {

    Optional<Room> findById(int idU);

    Optional<Room> findByIdAndHotelId(int id, Hotel hotelId);

}

/*   @Query(value = "SELECT MAX(r.room_number) FROM Room r",nativeQuery = true)
    int findMaxRoomNumber();*/