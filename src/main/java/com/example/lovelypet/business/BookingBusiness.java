package com.example.lovelypet.business;

import com.example.lovelypet.entity.*;
import com.example.lovelypet.exception.*;
import com.example.lovelypet.mapper.BookingMapper;
import com.example.lovelypet.model.BookingListResponse;
import com.example.lovelypet.model.BookingRequest;
import com.example.lovelypet.model.ConsiderBookingRequest;
import com.example.lovelypet.service.*;
import com.example.lovelypet.util.SecurityUtil;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;


@Service
public class BookingBusiness {

    private final PetService petService;

    private final BookingMapper bookingMapper;

    private final HotelService hotelService;

    private final RoomService roomService;

    private final UserService userService;

    private final ServiceHistoryService serviceHistoryService;
    private final BookingService bookingService;
    private final String path = "src/main/resources/imageUpload/payment";

    public BookingBusiness(PetService petService, BookingMapper bookingMapper, HotelService hotelService, RoomService roomService, UserService userService, ServiceHistoryService serviceHistoryService, BookingService bookingService) {
        this.petService = petService;
        this.bookingMapper = bookingMapper;
        this.hotelService = hotelService;
        this.roomService = roomService;
        this.userService = userService;
        this.serviceHistoryService = serviceHistoryService;
        this.bookingService = bookingService;
    }

    public String reserve(BookingRequest request) throws BaseException, IOException {

        //user
        Optional<String> opt = SecurityUtil.getCurrentUserId();
        if (opt.isEmpty()) {
            throw UserException.unauthorized();
        }

        String userId = opt.get();
        Optional<User> optUser = userService.findById(Integer.parseInt(userId));
        if (optUser.isEmpty()) {
            throw UserException.notFound();
        }

        //hotel
        Optional<Hotel> optHotel = hotelService.findById(request.getHotelId());
        if (optHotel.isEmpty()) {
            throw HotelException.notFound();
        }

        //room
        Optional<Room> optRoom = roomService.findById(request.getRoomId());
        if (optRoom.isEmpty()) {
            throw RoomException.notFound();
        }

        if (!optRoom.get().getStatus().equals("empty")) {
            throw BookingException.roomIsNotAvailable();
        }

        //pet
        Optional<Pet> optPet = petService.findById(request.getPetId());
        if (optPet.isEmpty()) {
            throw PetException.notFound();
        }

        //date start
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        ParsePosition pos = new ParsePosition(0);
        Date startDate = dateFormat.parse(request.getStart(), pos);

        //date end
        ParsePosition pos2 = new ParsePosition(0);
        Date endDate = dateFormat.parse(request.getEnd(), pos2);

        //date now
        LocalDateTime date = LocalDateTime.now();

        String fileImage = null;
        if (request.getFile() != null) {
            fileImage = uploadImage(request.getFile());
        }

        //add pet to database
        Booking response = bookingService.create(
                optUser.get(),
                optHotel.get(),
                optRoom.get(),
                optPet.get(),
                startDate,
                endDate,
                date,
                request.getPaymentMethod(),
                fileImage

        );

        return "Wait for the approval of booking number " + response.getId();
    }

    public String uploadImage(MultipartFile file) throws IOException, BaseException {

        //validate request
        if (file == null) {
            throw FileException.fileNull();
        }

        if (file.getSize() > 1048576 * 5) {
            throw FileException.fileMaxSize();
        }
        String contentType = file.getContentType();
        if (contentType == null) {
            throw FileException.unsupported();
        }

        List<String> supportedType = Arrays.asList("image/jpeg", "image/png");
        if (!supportedType.contains(contentType)) {
            throw FileException.unsupported();
        }

        // สร้างชื่อไฟล์ที่ไม่ซ้ำกัน
        String fileName = generateUniqueFileName(file.getOriginalFilename());

        String filePath = path + File.separator + fileName;
        //File filePath = new File(uploadDir, fileName);


        // สร้างไดเร็กทอรีถ้ายังไม่มี
        File directory = new File(path);
        if (!directory.exists()) {
            boolean success = directory.mkdirs();
            // ตรวจสอบผลลัพธ์
            if (!success) {
                throw FileException.failedToCreateDirectory();
            }
        }

        // Save the image file
        //file.transferTo(filePath);
        Path path = Paths.get(filePath);
        Files.write(path, file.getBytes());

        return fileName;
    }

    // สร้างชื่อไฟล์ที่ไม่ซ้ำกัน
    private String generateUniqueFileName(String originalFileName) {
        return UUID.randomUUID() + "_" + originalFileName;
    }

    public ResponseEntity<InputStreamResource> getImageById(int id) {
        Optional<Booking> imageEntity = bookingService.findById(id);
        if (imageEntity.isPresent()) {
            String filename = imageEntity.get().getPayment();
            File imageFile = new File(path + File.separator + filename);

            try {
                InputStreamResource resource = new InputStreamResource(new FileInputStream(imageFile));
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline;filename=" + filename)
                        .contentType(MediaType.IMAGE_JPEG)
                        .contentLength(imageFile.length())
                        .body(resource);
            } catch (IOException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
            }
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    public String getImageUrl(int id) throws BaseException {
        Optional<Booking> images = bookingService.findById(id); // ดึงข้อมูลรูปภาพทั้งหมดจากฐานข้อมูล
        if (images.isEmpty()) {
            throw BookingException.notFound();
        }
        return path + File.separator + images.get().getPayment();
    }

    //get data
    public List<BookingListResponse> listBooking(String state) throws BaseException {
        //Hotel
        Optional<String> opt = SecurityUtil.getCurrentUserId();
        if (opt.isEmpty()) {
            throw UserException.unauthorized();
        }
        String hotelId = opt.get();
        Optional<Hotel> optHotel = hotelService.findById(Integer.parseInt(hotelId));
        if (optHotel.isEmpty()) {
            throw HotelException.notFound();
        }
        Hotel hotel = optHotel.get();
        List<Booking> bookingList = bookingService.findByIdHotelAndState(hotel, state);
        if (bookingList.isEmpty()) {
            throw BookingException.notFound();
        }

        List<BookingListResponse> response = new ArrayList<>();
        for (Booking booking : bookingList) {
            BookingListResponse data = bookingMapper.toBookingListResponse(booking);
            response.add(data);
        }
        return response;
    }

    public List<BookingListResponse> allListBooking() throws BaseException {
        //Hotel
        Optional<String> opt = SecurityUtil.getCurrentUserId();
        if (opt.isEmpty()) {
            throw UserException.unauthorized();
        }
        String hotelId = opt.get();
        Optional<Hotel> optHotel = hotelService.findById(Integer.parseInt(hotelId));
        if (optHotel.isEmpty()) {
            throw HotelException.notFound();
        }
        Hotel hotel = optHotel.get();
        List<Booking> bookingList = bookingService.findByIdHotel(hotel);
        if (bookingList.isEmpty()) {
            throw BookingException.notFound();
        }

        List<BookingListResponse> response = new ArrayList<>();
        for (Booking booking : bookingList) {
            BookingListResponse data = bookingMapper.toBookingListResponse(booking);
            response.add(data);
        }
        return response;
    }

    public BookingListResponse getBooking(int id) throws BaseException {
        if (id == 0) {
            throw BookingException.idBookingIsNull();
        }
        Optional<Booking> opt = bookingService.findById(id);
        if (opt.isEmpty()) {
            throw BookingException.notFound();
        }
        Booking booking = opt.get();
        return bookingMapper.toBookingListResponse(booking);
    }

    //considerBooking
    public String considerBooking(ConsiderBookingRequest request) throws BaseException {
        Optional<Booking> opt = bookingService.findById(request.getId());
        if (opt.isEmpty()) {
            throw BookingException.notFound();
        }
        Booking booking = opt.get();
        booking.setState(request.getState());
        Booking updateBooking = bookingService.updateBooking(booking);
        String state = updateBooking.getState();
        String response = null;
        if (state.equals("approve")) {
            response = "Booking No. " + updateBooking.getId() + " has been approved.";
        }
        if (state.equals("disapproval")) {
            response = "Booking No. " + updateBooking.getId() + " has been disapproved.";
        }
        if (state.equals("complete")) {
            //service history record
            serviceHistoryService.create(updateBooking.getHotelId(), updateBooking.getUserId(), updateBooking);
            response = "Completed No." + updateBooking.getId() + " Booking service.";
        }
        return response;
    }

    //update
    public String updateProfile(BookingRequest request) throws BaseException, IOException {
        Optional<Booking> opt = bookingService.findById(request.getIdBooking());
        if (opt.isEmpty()) {
            throw BookingException.notFound();
        }
        Booking booking = opt.get();
        if (booking.getState().equals("waite")) {
            if (request.getRoomId() != 0) {
                Optional<Room> optRoom = roomService.findById(request.getRoomId());
                if (optRoom.isEmpty()) {
                    throw RoomException.notFound();
                }
                booking.setRoomId(optRoom.get());
            }

            if (request.getPetId() != 0) {
                Optional<Pet> optPet = petService.findById(request.getPetId());
                if (optPet.isEmpty()) {
                    throw PetException.notFound();
                }
                booking.setPetId(optPet.get());
            }
            if (Objects.nonNull(request.getStart())) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
                ParsePosition pos = new ParsePosition(0);
                Date startDate = dateFormat.parse(request.getStart(), pos);
                booking.setBookingStartDate(startDate);
            }
            if (Objects.nonNull(request.getEnd())) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
                ParsePosition pos = new ParsePosition(0);
                Date endDate = dateFormat.parse(request.getEnd(), pos);
                booking.setBookingStartDate(endDate);
            }
            if (Objects.nonNull(request.getPaymentMethod())) {
                if (Objects.nonNull(booking.getPayment())) {
                    throw BookingException.updatePaymentMethodFail();
                }
                booking.setPaymentMethod(request.getPaymentMethod());
            }
            if (Objects.nonNull(request.getFile())) {
                if (!booking.getPaymentMethod().equals("cash payment")) {
                    String fileImage = null;
                    if (request.getFile() != null) {
                        fileImage = uploadImage(request.getFile());
                        booking.setPayment(fileImage);
                    } else {
                        throw BookingException.createBookingPaymentNull();
                    }
                } else {
                    throw BookingException.wrongPaymentMethod();
                }
            }
            bookingService.updateBooking(booking);
            return "Update booking No." + booking.getId() + " completed.";
        } else {
            throw BookingException.updateFail();
        }
    }

    //cancel
    public String cancelBooking(int id) throws BaseException {
        if (id == 0) {
            throw BookingException.idBookingIsNull();
        }
        Optional<Booking> opt = bookingService.findById(id);
        if (opt.isEmpty()) {
            throw BookingException.notFound();
        }
        Booking booking = opt.get();
        booking.setState("cancel");
        bookingService.updateBooking(booking);
        return "Booking No." + booking.getId() + " has been cancelled.";
    }

    //delete
    public String deleteBooking(int id) throws BaseException {
        if (id == 0) {
            throw BookingException.idBookingIsNull();
        }
        Optional<Booking> opt = bookingService.findById(id);
        if (opt.isEmpty()) {
            throw BookingException.notFound();
        }
        Booking booking = opt.get();
        String fileName = booking.getPayment();
        String filePath = path + File.separator + fileName;

        // สร้างอ็อบเจ็กต์ File จาก path ของไฟล์
        File imageFile = new File(filePath);

        // ตรวจสอบว่าไฟล์มีอยู่จริงหรือไม่ และลบไฟล์ออกจากเครื่อง server
        if (imageFile.exists()) {
            boolean deleted = imageFile.delete();
            if (!deleted) {
                throw FileException.deleteImageFailed();
            }
        } else {
            throw FileException.deleteNoFile();
        }

        bookingService.deleteById(id);
        return "Booking deleted";
    }
    ///////////////////////////////

    ///////////////////////////////

}




