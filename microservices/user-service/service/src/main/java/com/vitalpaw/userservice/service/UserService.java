package com.vitalpaw.userservice.service;

import com.vitalpaw.userservice.model.Pet;
import com.vitalpaw.userservice.model.Sensor;
import com.vitalpaw.userservice.model.User;
import com.vitalpaw.userservice.repository.PetRepository;
import com.vitalpaw.userservice.repository.SensorRepository;
import com.vitalpaw.userservice.repository.UserRepository;
import com.vitalpaw.userservice.dto.UserDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PetRepository petRepository;

    @Autowired
    private SensorRepository sensorRepository;

    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    private final RestTemplate restTemplate = new RestTemplate();

    public User createUser(UserDTO userDTO) {
        User user = new User();
        user.setFirstName(userDTO.getFirstName());
        user.setLastName(userDTO.getLastName());
        user.setEmail(userDTO.getEmail());
        user.setPassword(userDTO.getPassword());
        user.setPhone(userDTO.getPhone());
        user.setCity(userDTO.getCity());
        user.setUsername(userDTO.getUsername());
        return userRepository.save(user);
    }

    public User loginUser(UserDTO userDTO) {
        return userRepository.findByEmailAndPassword(userDTO.getEmail(), userDTO.getPassword())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));
    }

    public User updateUser(Long id, UserDTO userDTO) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setFirstName(userDTO.getFirstName());
        user.setLastName(userDTO.getLastName());
        user.setEmail(userDTO.getEmail());
        user.setPassword(userDTO.getPassword());
        user.setPhone(userDTO.getPhone());
        user.setCity(userDTO.getCity());
        user.setUsername(userDTO.getUsername());
        return userRepository.save(user);
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public List<Pet> getPetsByUserId(Long userId) {
        return petRepository.findByOwnerId(userId);
    }

    public void prepareVetAiDataForUser(Long userId) {
        executorService.submit(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            List<Pet> pets = petRepository.findByOwnerId(userId);
            for (Pet pet : pets) {
                VetAiPetData data = new VetAiPetData();
                data.setPetId(pet.getId());
                data.setName(pet.getName());
                data.setBreed(pet.getBreed());
                data.setAge(Integer.parseInt(pet.getAge())); // Convertir String a Integer

                restTemplate.postForObject("http://localhost:8082/vet-ai/data", data, Void.class);
            }
        });
    }

    private static class VetAiPetData {
        private Long petId;
        private String name;
        private String breed;
        private Integer age;

        public Long getPetId() { return petId; }
        public void setPetId(Long petId) { this.petId = petId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getBreed() { return breed; }
        public void setBreed(String breed) { this.breed = breed; }
        public Integer getAge() { return age; }
        public void setAge(Integer age) { this.age = age; }
    }
}