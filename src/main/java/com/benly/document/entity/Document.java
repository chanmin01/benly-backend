package com.benly.document.entity;

import com.benly.global.entity.BaseEntity;
import com.benly.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "documents")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Document extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "storage_key", nullable = false, length = 500)
    private String storageKey;

    private Document(User user, String fileName, String storageKey) {
        this.user = user;
        this.fileName = fileName;
        this.storageKey = storageKey;
    }

    public static Document create(User user, String fileName, String storageKey) {
        return new Document(user, fileName, storageKey);
    }

    public void rename(String newFileName) {
        this.fileName = newFileName;
    }

}
