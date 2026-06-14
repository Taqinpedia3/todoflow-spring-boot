package com.taqin.todoapi.todo;

import com.taqin.todoapi.auth.AppUser;
import com.taqin.todoapi.auth.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TodoControllerTests {
    private static final String USER_EMAIL = "todo-user@example.com";
    private static final String OTHER_USER_EMAIL = "other-user@example.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TodoRepository todoRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @BeforeEach
    void setUp() {
        todoRepository.deleteAll();
        appUserRepository.deleteAll();
        appUserRepository.save(new AppUser("Todo User", USER_EMAIL, "hashed-password"));
        appUserRepository.save(new AppUser("Other User", OTHER_USER_EMAIL, "hashed-password"));
    }

    @Test
    void canCreateReadUpdateAndDeleteTodo() throws Exception {
        String response = mockMvc.perform(post("/api/todos")
                        .with(jwtFor(USER_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Belajar test",
                                  "completed": false,
                                  "dueAt": "2026-06-08T12:00:00Z"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Belajar test"))
                .andExpect(jsonPath("$.dueAt").value("2026-06-08T12:00:00Z"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String id = response.replaceAll(".*\"id\":(\\d+).*", "$1");

        mockMvc.perform(get("/api/todos/{id}", id).with(jwtFor(USER_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Belajar test"));

        mockMvc.perform(put("/api/todos/{id}", id)
                        .with(jwtFor(USER_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Belajar test Spring",
                                  "completed": true,
                                  "dueAt": "2026-06-08T13:30:00Z"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(true))
                .andExpect(jsonPath("$.dueAt").value("2026-06-08T13:30:00Z"));

        mockMvc.perform(delete("/api/todos/{id}", id).with(jwtFor(USER_EMAIL)))
                .andExpect(status().isNoContent());
    }

    @Test
    void rejectsBlankTitle() throws Exception {
        mockMvc.perform(post("/api/todos")
                        .with(jwtFor(USER_EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Data tidak valid"))
                .andExpect(jsonPath("$.errors.title").value("Title wajib diisi"));
    }

    @Test
    void returnsNotFoundErrorWhenTodoDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/todos/{id}", 999).with(jwtFor(USER_EMAIL)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Todo dengan id 999 tidak ditemukan"));
    }

    @Test
    void listsOnlyTodosOwnedByAuthenticatedUser() throws Exception {
        AppUser user = appUserRepository.findByEmail(USER_EMAIL).orElseThrow();
        AppUser otherUser = appUserRepository.findByEmail(OTHER_USER_EMAIL).orElseThrow();
        todoRepository.save(new Todo("Todo milik saya", user));
        todoRepository.save(new Todo("Todo milik orang lain", otherUser));

        mockMvc.perform(get("/api/todos").with(jwtFor(USER_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Todo milik saya"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void cannotReadTodoOwnedByAnotherUser() throws Exception {
        AppUser otherUser = appUserRepository.findByEmail(OTHER_USER_EMAIL).orElseThrow();
        Todo otherTodo = todoRepository.save(new Todo("Rahasia user lain", otherUser));

        mockMvc.perform(get("/api/todos/{id}", otherTodo.getId()).with(jwtFor(USER_EMAIL)))
                .andExpect(status().isNotFound());
    }

    @Test
    void supportsPaginationAndSorting() throws Exception {
        AppUser user = appUserRepository.findByEmail(USER_EMAIL).orElseThrow();
        todoRepository.save(new Todo("Alpha", user));
        todoRepository.save(new Todo("Charlie", user));
        todoRepository.save(new Todo("Bravo", user));

        mockMvc.perform(get("/api/todos")
                        .param("page", "0")
                        .param("size", "2")
                        .param("sortBy", "title")
                        .param("direction", "asc")
                        .with(jwtFor(USER_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].title").value("Alpha"))
                .andExpect(jsonPath("$.content[1].title").value("Bravo"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.last").value(false));
    }

    @Test
    void filtersByCompletedStatus() throws Exception {
        AppUser user = appUserRepository.findByEmail(USER_EMAIL).orElseThrow();
        Todo done = new Todo("Todo selesai", user);
        done.setCompleted(true);
        todoRepository.save(done);
        todoRepository.save(new Todo("Todo belum selesai", user));

        mockMvc.perform(get("/api/todos")
                        .param("completed", "true")
                        .with(jwtFor(USER_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Todo selesai"))
                .andExpect(jsonPath("$.content[0].completed").value(true))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void searchesByTitleWithinAuthenticatedUsersTodos() throws Exception {
        AppUser user = appUserRepository.findByEmail(USER_EMAIL).orElseThrow();
        AppUser otherUser = appUserRepository.findByEmail(OTHER_USER_EMAIL).orElseThrow();
        todoRepository.save(new Todo("Belajar Spring Security", user));
        todoRepository.save(new Todo("Belanja sayur", user));
        todoRepository.save(new Todo("Belajar milik user lain", otherUser));

        mockMvc.perform(get("/api/todos")
                        .param("q", "belajar")
                        .param("sortBy", "title")
                        .param("direction", "asc")
                        .with(jwtFor(USER_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Belajar Spring Security"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    private JwtRequestPostProcessor jwtFor(String email) {
        return jwt().jwt(jwt -> jwt.subject(email));
    }
}
