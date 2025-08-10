package org.example.jfranalyzerbackend.entity.shared.file;

import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Table(name = "transferring_files")
@Entity
public class TransferringFileEntity extends BaseFileEntity {

    public static final int MAX_FAILURE_MESSAGE_LENGTH = 1024;

//    @Column(nullable = false)
//    @Enumerated(EnumType.STRING)
//    private FileTransferState transferState;

    @Column(nullable = false)
    private long totalSize;

    @Column(nullable = false)
    private long transferredSize;

    @Column(length = MAX_FAILURE_MESSAGE_LENGTH)
    private String failureMessage;

    @Column(nullable = false)
    @UpdateTimestamp
    private LocalDateTime lastModifiedTime;

    public long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }

    public long getTransferredSize() {
        return transferredSize;
    }

    public void setTransferredSize(long transferredSize) {
        this.transferredSize = transferredSize;
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    public void setFailureMessage(String failureMessage) {
        this.failureMessage = failureMessage;
    }

    public LocalDateTime getLastModifiedTime() {
        return lastModifiedTime;
    }

    public void setLastModifiedTime(LocalDateTime lastModifiedTime) {
        this.lastModifiedTime = lastModifiedTime;
    }
}
