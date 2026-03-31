package com.votify.persistence;

import com.votify.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByProjectId(Long projectId);

    List<Comment> findByProjectIdIn(List<Long> projectIds);

    void deleteByProjectIdIn(List<Long> projectIds);
}
