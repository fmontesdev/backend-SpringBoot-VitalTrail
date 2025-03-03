package com.springboot.vitaltrail.domain.user;

import com.springboot.vitaltrail.domain.user.admin.AdminEntity;
import com.springboot.vitaltrail.domain.user.client.ClientEntity;

// import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.Builder;
import jakarta.persistence.*;
import java.util.UUID;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users")
// Identificador único. Hace que otras entidades puedan referenciar a esta y viceversa sin entrar en bucle en la serialización
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "idUser")
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id_user")
    private UUID idUser;

    @Column(name = "email", nullable = false, unique = true, length = 180)
    private String email;

    @Column(name = "username", nullable = false, unique = true, length = 32)
    private String username;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "name", nullable = false, length = 64)
    private String name;

    @Column(name = "surname", nullable = false, length = 132)
    private String surname;

    @Column(name = "birthday")
    private LocalDateTime birthday;

    @Column(name = "bio", length = 255)
    private String bio;

    @Column(name = "img_user", length = 255)
    private String imgUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false)
    private Role rol;

    @Column(name = "is_active")
    private Boolean isActive = false;

    @Column(name = "is_deleted") 
    private Boolean isDeleted = false;

    @Column(name = "is_premium") 
    private Boolean isPremium = false;

    @OneToOne(
        mappedBy = "user",
        fetch = FetchType.LAZY,
        cascade = CascadeType.ALL,
        orphanRemoval = true)
    private AdminEntity admin;

    @OneToOne(
        mappedBy = "user",
        fetch = FetchType.LAZY,
        cascade = CascadeType.ALL,
        orphanRemoval = true)
    private ClientEntity client;

    public enum Role {
        ROLE_ADMIN, ROLE_CLIENT
    }

    public void generateUUID() {
        if (this.idUser == null) {
            this.idUser = UUID.randomUUID();
        }
    }

    @Builder
    public UserEntity(
        UUID idUser,
        String email,
        String username,
        String password,
        String name,
        String surname,
        LocalDateTime birthday,
        String bio,
        String imgUser,
        Role rol,
        Boolean isActive,
        Boolean isDeleted,
        Boolean isPremium
    ){
        this.idUser = idUser;
        this.email = email;
        this.username = username;
        this.password = password;
        this.name = name;
        this.surname = surname;
        this.birthday = birthday;
        this.bio = bio;
        this.imgUser = imgUser;
        this.rol = rol;
        this.isActive = isActive;
        this.isDeleted = isDeleted;
        this.isPremium = isPremium;
    }
}
