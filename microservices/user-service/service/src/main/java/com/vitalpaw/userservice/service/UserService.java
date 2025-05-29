package com.vitalpaw.userservice.service;

import com.vitalpaw.userservice.model.Pet;
import com.vitalpaw.userservice.model.Sensor;
import com.vitalpaw.userservice.model.User;
import com.vitalpaw.userservice.repository.PetRepository;
import com.vitalpaw.userservice.repository.SensorRepository;
import com.vitalpaw.userservice.repository.UserRepository;
import com.vitalpaw.userservice.dto.UserDTO;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
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

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private JwtService jwtService;

    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    private final RestTemplate restTemplate = new RestTemplate();

    public User createUser(UserDTO userDTO) {
        User user = new User();
        user.setFirstName(userDTO.getFirstName());
        user.setLastName(userDTO.getLastName());
        user.setEmail(userDTO.getEmail());
        user.setPassword(BCrypt.hashpw(userDTO.getPassword(), BCrypt.gensalt()));
        user.setPhone(userDTO.getPhone());
        user.setCity(userDTO.getCity());
        user.setUsername(userDTO.getUsername());
        user.setVerificationToken(UUID.randomUUID().toString());
        user.setVerified(false);
        User savedUser = userRepository.save(user);
        sendVerificationEmail(savedUser);
        return savedUser;
    }

    public String loginUser(UserDTO userDTO) {
        User user = userRepository.findByEmail(userDTO.getEmail())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        if (!BCrypt.checkpw(userDTO.getPassword(), user.getPassword())) {
            throw new RuntimeException("Credenciales inválidas");
        }
        if (!user.isVerified()) {
            throw new RuntimeException("Cuenta no verificada");
        }
        return jwtService.generateToken(user.getUsername());
    }

    public User updateUser(Long id, UserDTO userDTO) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setFirstName(userDTO.getFirstName());
        user.setLastName(userDTO.getLastName());
        user.setEmail(userDTO.getEmail());
        user.setPassword(BCrypt.hashpw(userDTO.getPassword(), BCrypt.gensalt()));
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
                data.setAge(parseAge(pet.getAge()));
                restTemplate.postForObject("http://vet-ai-service:8001/vet-ai/data", data, Void.class);
            }
        });
    }

    private Integer parseAge(String age) {
        if (age == null || !age.contains("años")) return 0;
        return Integer.parseInt(age.split(" años")[0].trim());
    }

    public void sendVerificationEmail(User user) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject("Verifica tu cuenta - VitalPaw");
        message.setText(getVerificationEmailBody(user.getVerificationToken()));
        mailSender.send(message);
    }

    public void sendResetPasswordEmail(User user) {
        String resetToken = UUID.randomUUID().toString();
        user.setVerificationToken(resetToken);
        userRepository.save(user);
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject("Recuperación de Contraseña - VitalPaw");
        message.setText(getResetPasswordEmailBody(resetToken));
        mailSender.send(message);
    }

    private String getVerificationEmailBody(String token) {
        return "<html><body>" +
                "<h2>Verifica tu cuenta</h2>" +
                "<p>Gracias por registrarte en VitalPaw. Haz clic en el botón para verificar tu correo:</p>" +
                "<a href='http://vitalpaw.duckdns.org/users/verify?token=" + token + "' style='display:inline-block;padding:10px 20px;background-color:#4CAF50;color:white;text-decoration:none;border-radius:5px;'>Verificar Cuenta</a>" +
                "</body></html>";
    }

    private String getResetPasswordEmailBody(String token) {
        return "<html><body>" +
                "<h2>Recuperación de Contraseña</h2>" +
                "<p>Haz clic en el botón para restablecer tu contraseña:</p>" +
                "<a href='http://vitalpaw.duckdns.org/users/reset-password?token=" + token + "' style='display:inline-block;padding:10px 20px;background-color:#4CAF50;color:white;text-decoration:none;border-radius:5px;'>Restablecer Contraseña</a>" +
                "</body></html>";
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    public User findByVerificationToken(String token) {
        return userRepository.findByVerificationToken(token).orElse(null);
    }

    public User save(User user) {
        return userRepository.save(user);
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