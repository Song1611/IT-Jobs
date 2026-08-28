package com.itjob.unit.mapper;

import com.itjob.dto.response.CommentResponse;
import com.itjob.entity.Comment;
import com.itjob.entity.Post;
import com.itjob.entity.User;
import com.itjob.mapper.CommentMapper;
import com.itjob.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Unit - CommentMapper")
class CommentMapperTest {

    private final CommentMapper mapper = Mappers.getMapper(CommentMapper.class);

    @BeforeEach
    void wireUsedMappers() {
        ReflectionTestUtils.setField(mapper, "userMapper", Mappers.getMapper(UserMapper.class));
    }

    @Test
    @DisplayName("toCommentResponse -> maps the author as the user")
    void toCommentResponseMapsAuthorAsUser() {
        User author = User.builder().id(UUID.randomUUID()).email("author@example.com").build();
        Post post = Post.builder().id(UUID.randomUUID()).build();
        Comment comment = Comment.builder().id(UUID.randomUUID()).post(post).author(author).content("Nice!").build();

        CommentResponse response = mapper.toCommentResponse(comment);

        assertThat(response.getUser().getEmail()).isEqualTo("author@example.com");
        assertThat(response.getContent()).isEqualTo("Nice!");
    }
}