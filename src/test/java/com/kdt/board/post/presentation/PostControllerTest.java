package com.kdt.board.post.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kdt.board.post.application.PostService;
import com.kdt.board.post.application.dto.request.EditPostRequestDto;
import com.kdt.board.post.application.dto.request.WritePostRequestDto;
import com.kdt.board.post.application.dto.response.PostResponseDto;
import com.kdt.board.post.presentation.dto.response.PostResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureRestDocs
@WebMvcTest(PostController.class)
public class PostControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    PostService postService;

    @Nested
    @DisplayName("Post 작성 요청이 들어올 때")
    class Describe_write_post {

        final String url = "/api/posts";

        @Nested
        @DisplayName("userId가 존재하지 않으면")
        class Context_with_null_userId {

            @Test
            @DisplayName("BadRequest를 응답한다.")
            void It_response_BadRequest() throws Exception {
                //given
                final HashMap<String, Object> requestMap = new HashMap<>();
                requestMap.put("title", "test");
                requestMap.put("content", "test");

                //when
                final ResultActions resultActions = writePostPerform(url, requestMap);

                //then
                resultActions.andExpect(status().isBadRequest());
            }
        }

        @Nested
        @DisplayName("userId가 0 이거나 음수라면")
        class Context_with_negative_userId {

            @ParameterizedTest
            @ValueSource(longs = {0L, -1L})
            @DisplayName("BadRequest를 응답한다.")
            void It_response_BadRequest(Long userId) throws Exception {
                //given
                final HashMap<String, Object> requestMap = new HashMap<>();
                requestMap.put("userId", userId);
                requestMap.put("title", "test");
                requestMap.put("content", "test");

                //when
                final ResultActions resultActions = writePostPerform(url, requestMap);

                //then
                resultActions.andExpect(status().isBadRequest());
            }
        }

        @Nested
        @DisplayName("userId가 문자열이라면")
        class Context_with_string_type_userId {

            @Test
            @DisplayName("BadRequest를 응답한다.")
            void It_response_BadRequest() throws Exception {
                //given
                final HashMap<String, Object> requestMap = new HashMap<>();
                requestMap.put("userId", "test");
                requestMap.put("title", "test");
                requestMap.put("content", "test");

                //when
                final ResultActions resultActions = writePostPerform(url, requestMap);

                //then
                resultActions.andExpect(status().isBadRequest());
            }
        }

        @Nested
        @DisplayName("title이 존재하지 않거나 빈 값이면")
        class Context_with_null_or_empty_title {

            @ParameterizedTest
            @NullAndEmptySource
            @DisplayName("BadRequest를 응답한다.")
            void It_response_BadRequest(String title) throws Exception {
                //given
                final HashMap<String, Object> requestMap = new HashMap<>();
                requestMap.put("userId", 1L);
                requestMap.put("title", title);
                requestMap.put("content", "test");

                //when
                final ResultActions resultActions = writePostPerform(url, requestMap);

                //then
                resultActions.andExpect(status().isBadRequest());
            }
        }

        @Nested
        @DisplayName("content가 존재하지 않거나 빈 값이면")
        class Context_with_null_or_empty_content {

            @ParameterizedTest
            @NullAndEmptySource
            @DisplayName("BadRequest를 응답한다.")
            void It_response_BadRequest(String content) throws Exception {
                //given
                final HashMap<String, Object> requestMap = new HashMap<>();
                requestMap.put("userId", 1L);
                requestMap.put("title", "test");
                requestMap.put("content", content);

                //when
                final ResultActions resultActions = writePostPerform(url, requestMap);

                //then
                resultActions.andExpect(status().isBadRequest());
            }
        }

        @Nested
        @DisplayName("유효한 userId, title, content이면")
        class Context_with_valid_userId_title_and_content {

            @Test
            @DisplayName("Created를 응답한다.")
            void It_response_Created() throws Exception {
                //given
                final HashMap<String, Object> requestMap = new HashMap<>();
                requestMap.put("userId", 1L);
                requestMap.put("title", "test");
                requestMap.put("content", "test");

                //when
                final ResultActions resultActions = writePostPerform(url, requestMap);

                //then
                verify(postService, times(1)).write(any(WritePostRequestDto.class));
                resultActions.andExpect(status().isCreated())
                        .andDo(document("post-create",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestFields(
                                        fieldWithPath("userId").type(JsonFieldType.NUMBER).description("유저 Id"),
                                        fieldWithPath("title").type(JsonFieldType.STRING).description("제목"),
                                        fieldWithPath("content").type(JsonFieldType.STRING).description("내용")
                                )
                        ));
            }
        }
    }

    @Nested
    @DisplayName("Post 다건 조회 요청이 들어올 때")
    class Describe_getAll_post {

        final String url = "/api/posts";

        @Nested
        @DisplayName("정상적이라면")
        class Context_with_normally {

            @Test
            @DisplayName("Ok를 응답한다.")
            void It_response_Ok() throws Exception {
                //given
                final PostResponseDto firstPostResponseDto = PostResponseDto.builder()
                        .id(1L)
                        .title("first test")
                        .content("first test")
                        .author("first test")
                        .createdAt(LocalDateTime.of(2022,
                                5,
                                22,
                                00,
                                00))
                        .build();
                final PostResponseDto secondPostResponseDto = PostResponseDto.builder()
                        .id(2L)
                        .title("second test")
                        .content("second test")
                        .author("second test")
                        .createdAt(LocalDateTime.of(2022,
                                5,
                                23,
                                00,
                                00))
                        .build();
                final List<PostResponseDto> postResponseDtoList = List.of(firstPostResponseDto, secondPostResponseDto);
                final PageRequest pageRequest = PageRequest.of(0,
                        2,
                        Sort.Direction.DESC,
                        "createdAt");
                doReturn(postResponseDtoList).when(postService).getAll(any(Pageable.class));

                // when
                final ResultActions resultActions = mockMvc.perform(get(url));

                //then
                verify(postService, times(1)).getAll(any(Pageable.class));
                resultActions.andExpect(status().isOk())
                        .andDo(document("post-read-multiple",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        responseFields(
                                fieldWithPath("[].id").type(JsonFieldType.NUMBER).description("게시물 id"),
                                fieldWithPath("[].title").type(JsonFieldType.STRING).description("제목"),
                                fieldWithPath("[].content").type(JsonFieldType.STRING).description("내용"),
                                fieldWithPath("[].author").type(JsonFieldType.STRING).description("글쓴이"),
                                fieldWithPath("[].createdAt").type(JsonFieldType.STRING).description("게시물 작성 시간")
                        )
                ));
            }
        }
    }

    @Nested
    @DisplayName("Post 단건 조회 요청이 들어올 때")
    class Describe_getOne_post {

        final String url = "/api/posts/{id}";

        @Nested
        @DisplayName("정상적이라면")
        class Context_with_normally {

            @Test
            @DisplayName("조회된 Post와 Ok를 응답한다.")
            void It_response_Ok() throws Exception {
                //given
                final Long id = 1L;
                final PostResponseDto postResponseDto = PostResponseDto.builder()
                        .id(1L)
                        .title("test")
                        .content("test")
                        .author("test")
                        .createdAt(LocalDateTime.of(2022, 5, 22, 00, 00))
                        .build();
                final PostResponse postResponse = PostResponse.from(postResponseDto);
                doReturn(postResponseDto).when(postService).getOne(id);

                // when
                final ResultActions resultActions = mockMvc.perform(
                        RestDocumentationRequestBuilders.get(url, id)
                );

                //then
                verify(postService, times(1)).getOne(id);
                resultActions.andExpect(status().isOk())
                        .andExpect(jsonPath("id").value(postResponse.getId()))
                        .andExpect(jsonPath("title").value(postResponse.getTitle()))
                        .andExpect(jsonPath("content").value(postResponse.getContent()))
                        .andExpect(jsonPath("author").value(postResponse.getAuthor()))
                        .andDo(document("post-read-one",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                pathParameters(
                                        parameterWithName("id").description("Post Id")
                                ),
                                responseFields(
                                        fieldWithPath("id").type(JsonFieldType.NUMBER).description("게시물 id"),
                                        fieldWithPath("title").type(JsonFieldType.STRING).description("제목"),
                                        fieldWithPath("content").type(JsonFieldType.STRING).description("내용"),
                                        fieldWithPath("author").type(JsonFieldType.STRING).description("글쓴이"),
                                        fieldWithPath("createdAt").type(JsonFieldType.STRING).description("게시물 작성 시간")
                                )
                        ));
            }
        }
    }

    @Nested
    @DisplayName("Post 편집 요청이 들어올 때")
    class Describe_edit_post {

        final String url = "/api/posts/{id}";
        final Long id = 1L;

        @Nested
        @DisplayName("userId가 존재하지 않으면")
        class Context_with_null_userId {

            @Test
            @DisplayName("BadRequest를 응답한다.")
            void It_response_BadRequest() throws Exception {
                //given
                final HashMap<String, Object> requestMap = new HashMap<>();
                requestMap.put("title", "test");
                requestMap.put("content", "test");

                //when
                final ResultActions resultActions = editPostPerform(url, id, requestMap);

                //then
                resultActions.andExpect(status().isBadRequest());
            }
        }

        @Nested
        @DisplayName("userId가 0 이거나 음수라면")
        class Context_with_negative_userId {

            @ParameterizedTest
            @ValueSource(longs = {0L, -1L})
            @DisplayName("BadRequest를 응답한다.")
            void It_response_BadRequest(Long userId) throws Exception {
                //given
                final HashMap<String, Object> requestMap = new HashMap<>();
                requestMap.put("userId", userId);
                requestMap.put("title", "test");
                requestMap.put("content", "test");

                //when
                final ResultActions resultActions = editPostPerform(url, id, requestMap);

                //then
                resultActions.andExpect(status().isBadRequest());
            }
        }

        @Nested
        @DisplayName("userId가 문자열이라면")
        class Context_with_string_type_userId {

            @Test
            @DisplayName("BadRequest를 응답한다.")
            void It_response_BadRequest() throws Exception {
                //given
                final HashMap<String, Object> requestMap = new HashMap<>();
                requestMap.put("userId", "test");
                requestMap.put("title", "test");
                requestMap.put("content", "test");

                //when
                final ResultActions resultActions = editPostPerform(url, id, requestMap);

                //then
                resultActions.andExpect(status().isBadRequest());
            }
        }

        @Nested
        @DisplayName("title이 존재하지 않거나 빈 값이면")
        class Context_with_null_or_empty_title {

            @ParameterizedTest
            @NullAndEmptySource
            @DisplayName("BadRequest를 응답한다.")
            void It_response_BadRequest(String title) throws Exception {
                //given
                final HashMap<String, Object> requestMap = new HashMap<>();
                requestMap.put("userId", 1L);
                requestMap.put("title", title);
                requestMap.put("content", "test");

                //when
                final ResultActions resultActions = editPostPerform(url, id, requestMap);

                //then
                resultActions.andExpect(status().isBadRequest());
            }
        }

        @Nested
        @DisplayName("content가 존재하지 않거나 빈 값이면")
        class Context_with_null_or_empty_content {

            @ParameterizedTest
            @NullAndEmptySource
            @DisplayName("BadRequest를 응답한다.")
            void It_response_BadRequest(String content) throws Exception {
                //given
                final HashMap<String, Object> requestMap = new HashMap<>();
                requestMap.put("userId", 1L);
                requestMap.put("title", "test");
                requestMap.put("content", content);

                //when
                final ResultActions resultActions = editPostPerform(url, id, requestMap);

                //then
                resultActions.andExpect(status().isBadRequest());
            }
        }

        @Nested
        @DisplayName("유효한 userId, title, content이면")
        class Context_with_valid_userId_title_and_content {

            @Test
            @DisplayName("Created를 응답한다.")
            void It_response_Created() throws Exception {
                //given
                final HashMap<String, Object> requestMap = new HashMap<>();
                requestMap.put("userId", 1L);
                requestMap.put("title", "test");
                requestMap.put("content", "test");

                //when
                final ResultActions resultActions = editPostPerform(url, id, requestMap);

                //then
                verify(postService, times(1)).edit(any(EditPostRequestDto.class));
                resultActions.andExpect(status().isOk())
                        .andDo(document("post-edit",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                pathParameters(
                                        parameterWithName("id").description("Post Id")
                                ),
                                requestFields(
                                        fieldWithPath("userId").type(JsonFieldType.NUMBER).description("유저 Id"),
                                        fieldWithPath("title").type(JsonFieldType.STRING).description("제목"),
                                        fieldWithPath("content").type(JsonFieldType.STRING).description("내용")
                                )
                        ));
            }
        }
    }

    private ResultActions writePostPerform(final String url, final HashMap<String, Object> requestMap) throws Exception {
        return mockMvc.perform(post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestMap)));
    }

    private ResultActions editPostPerform(final String url, final Long id, final HashMap<String, Object> requestMap) throws Exception {
        return mockMvc.perform(RestDocumentationRequestBuilders.put(url, id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestMap)));
    }
}