package dev.vk.bot.entities;

import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(schema = "public", name = "lobby")
public class Lobby {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private int peerId;

    @Column(unique = true)
    private int chatId;

    @Column(updatable = false)
    LocalDateTime invitedDate;

    @OneToOne
    Game game;

    public Lobby() {

    }

    public Lobby(int chatId, int peerId) {
        this.chatId = chatId;
        this.peerId = peerId;
        this.invitedDate = LocalDateTime.now();
    }

    public boolean isGameRunning() {
        return game != null;
    }
}