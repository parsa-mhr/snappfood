package org.example.User;
import java.util.ArrayList;
import jakarta.persistence.* ;
@Entity
@Table (name = "users")
@Inheritance (strategy = InheritanceType.JOINED)
public abstract class User {
    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private long id ;
    @Column (nullable = false)
    protected String name;
    protected String email;
    protected String password;
    public User(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
    }

    public User() {

    }

    public long getId () {
        return id ;
    }
}
