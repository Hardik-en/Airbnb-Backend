package com.Hardik.projects.AirbnbApp.entity;

import com.Hardik.projects.AirbnbApp.entity.enums.Gender;
import com.Hardik.projects.AirbnbApp.entity.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "app-user")
public class User implements UserDetails {
     @Id
     @GeneratedValue(strategy = GenerationType.IDENTITY)
     private Long id;

     @ElementCollection(fetch = FetchType.EAGER)
     @Enumerated(EnumType.STRING)
     private Set<Role> roles;

     private String name;

     @Column(nullable = false,unique = true)
     private String email;

     @Column(nullable = false)
     private String password;

     private LocalDate dateOfBirth;

     @Enumerated(EnumType.STRING)
     private Gender gender;

     @Override
     public Collection<? extends GrantedAuthority> getAuthorities() {
          return roles.stream()
                  .map(role -> new SimpleGrantedAuthority("ROLE_"+role.name()))
                  .collect(Collectors.toSet());
     }

     @Override
     public String getUsername() {
          return email;
     }

     @Override
     public boolean equals(Object o) {
          if (!(o instanceof User user)) return false;
          return Objects.equals(id, user.id);
     }

     @Override
     public int hashCode() {
          return Objects.hashCode(id);
     }

     @Override
     public boolean isEnabled() {
          return true;
     }

     @Override
     public boolean isAccountNonExpired() {
          return true;
     }

     @Override
     public boolean isAccountNonLocked() {
          return true;
     }

     @Override
     public boolean isCredentialsNonExpired() {
          return true;
     }

}
