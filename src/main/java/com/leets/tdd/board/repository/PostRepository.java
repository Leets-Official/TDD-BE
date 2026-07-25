package com.leets.tdd.board.repository;

import com.leets.tdd.board.domain.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    // 게시글 목록을 최신순으로 가져온다.
    // createdAt이 같은 글이 있을 수 있으므로 id를 보조 기준으로 두어 순서를 고정한다.
    List<Post> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);
}
