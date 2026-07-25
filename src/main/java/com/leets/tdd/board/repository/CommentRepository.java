package com.leets.tdd.board.repository;

import com.leets.tdd.board.domain.Comment;
import com.leets.tdd.board.dto.CommentCount;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    // 특정 게시글의 댓글을 오래된 순으로 가져온다.
    List<Comment> findByPostIdOrderByCreatedAtAscIdAsc(Long postId, Pageable pageable);

    // 게시글 하나의 댓글 수
    long countByPostId(Long postId);

    // 여러 게시글의 댓글 수를 한 번의 쿼리로 함께 조회한다(목록에서 게시글마다 세지 않도록).
    @Query("SELECT new com.leets.tdd.board.dto.CommentCount(c.postId, COUNT(c)) "
            + "FROM Comment c WHERE c.postId IN :postIds GROUP BY c.postId")
    List<CommentCount> countGroupedByPostIds(@Param("postIds") List<Long> postIds);
}
