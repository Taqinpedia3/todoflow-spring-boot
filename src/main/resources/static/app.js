const state = {
    token: localStorage.getItem("todo_token"),
    user: JSON.parse(localStorage.getItem("todo_user") || "null"),
    page: 0,
    size: 8,
    last: true,
    todos: [],
    editingTodoId: null,
    notifiedAlarms: JSON.parse(localStorage.getItem("todo_notified_alarms") || "{}")
};

const elements = {
    authView: document.querySelector("#authView"),
    dashboardView: document.querySelector("#dashboardView"),
    loginTab: document.querySelector("#loginTab"),
    registerTab: document.querySelector("#registerTab"),
    loginForm: document.querySelector("#loginForm"),
    registerForm: document.querySelector("#registerForm"),
    authMessage: document.querySelector("#authMessage"),
    userName: document.querySelector("#userName"),
    notificationButton: document.querySelector("#notificationButton"),
    logoutButton: document.querySelector("#logoutButton"),
    todoForm: document.querySelector("#todoForm"),
    todoTitle: document.querySelector("#todoTitle"),
    todoDueAt: document.querySelector("#todoDueAt"),
    searchInput: document.querySelector("#searchInput"),
    filterSelect: document.querySelector("#filterSelect"),
    sortSelect: document.querySelector("#sortSelect"),
    todoList: document.querySelector("#todoList"),
    todoMessage: document.querySelector("#todoMessage"),
    totalCount: document.querySelector("#totalCount"),
    doneCount: document.querySelector("#doneCount"),
    activeCount: document.querySelector("#activeCount"),
    prevPage: document.querySelector("#prevPage"),
    nextPage: document.querySelector("#nextPage"),
    pageInfo: document.querySelector("#pageInfo"),
    editDialog: document.querySelector("#editDialog"),
    editForm: document.querySelector("#editForm"),
    editTitle: document.querySelector("#editTitle"),
    editDueAt: document.querySelector("#editDueAt"),
    editCompleted: document.querySelector("#editCompleted"),
    cancelEdit: document.querySelector("#cancelEdit")
};

function setMessage(target, text, type = "") {
    target.textContent = text;
    target.className = `message ${type}`.trim();
}

function showLoginMode(mode) {
    const isLogin = mode === "login";
    elements.loginTab.classList.toggle("active", isLogin);
    elements.registerTab.classList.toggle("active", !isLogin);
    elements.loginForm.classList.toggle("hidden", !isLogin);
    elements.registerForm.classList.toggle("hidden", isLogin);
    setMessage(elements.authMessage, "");
}

function setSession(loginResponse) {
    state.token = loginResponse.accessToken;
    state.user = loginResponse.user;
    localStorage.setItem("todo_token", state.token);
    localStorage.setItem("todo_user", JSON.stringify(state.user));
}

function clearSession() {
    state.token = null;
    state.user = null;
    state.todos = [];
    localStorage.removeItem("todo_token");
    localStorage.removeItem("todo_user");
}

function renderApp() {
    const loggedIn = Boolean(state.token);
    elements.authView.classList.toggle("hidden", loggedIn);
    elements.dashboardView.classList.toggle("hidden", !loggedIn);
    elements.userName.textContent = state.user?.name || state.user?.email || "User";
    updateNotificationButton();
    if (loggedIn) {
        loadTodos();
        checkAlarms();
    }
}

async function api(path, options = {}) {
    const headers = {
        "Content-Type": "application/json",
        ...options.headers
    };

    if (state.token) {
        headers.Authorization = `Bearer ${state.token}`;
    }

    const response = await fetch(path, { ...options, headers });
    if (response.status === 204) {
        return null;
    }

    const data = await response.json().catch(() => null);
    if (!response.ok) {
        const details = data?.errors ? Object.values(data.errors).join(", ") : "";
        throw new Error(details || data?.message || "Request gagal");
    }
    return data;
}

async function register(event) {
    event.preventDefault();
    const name = document.querySelector("#registerName").value.trim();
    const email = document.querySelector("#registerEmail").value.trim();
    const password = document.querySelector("#registerPassword").value;

    try {
        await api("/api/auth/register", {
            method: "POST",
            body: JSON.stringify({ name, email, password })
        });
        elements.loginForm.querySelector("#loginEmail").value = email;
        elements.loginForm.querySelector("#loginPassword").value = password;
        showLoginMode("login");
        setMessage(elements.authMessage, "Akun berhasil dibuat. Silakan login.", "success");
    } catch (error) {
        setMessage(elements.authMessage, error.message, "error");
    }
}

async function login(event) {
    event.preventDefault();
    const email = document.querySelector("#loginEmail").value.trim();
    const password = document.querySelector("#loginPassword").value;

    try {
        const data = await api("/api/auth/login", {
            method: "POST",
            body: JSON.stringify({ email, password })
        });
        setSession(data);
        setMessage(elements.authMessage, "");
        renderApp();
    } catch (error) {
        setMessage(elements.authMessage, error.message, "error");
    }
}

function queryParams() {
    const [sortBy, direction] = elements.sortSelect.value.split(",");
    const params = new URLSearchParams({
        page: state.page,
        size: state.size,
        sortBy,
        direction
    });

    const completed = elements.filterSelect.value;
    const q = elements.searchInput.value.trim();
    if (completed) {
        params.set("completed", completed);
    }
    if (q) {
        params.set("q", q);
    }
    return params;
}

async function loadTodos() {
    try {
        const page = await api(`/api/todos?${queryParams()}`);
        state.todos = page.content;
        state.page = page.page;
        state.last = page.last;
        renderTodos(page);
        setMessage(elements.todoMessage, "");
    } catch (error) {
        if (error.message.includes("Unauthorized")) {
            clearSession();
            renderApp();
        }
        setMessage(elements.todoMessage, error.message, "error");
    }
}

function renderTodos(page) {
    const total = page.totalElements;
    const done = state.todos.filter((todo) => todo.completed).length;
    elements.totalCount.textContent = total;
    elements.doneCount.textContent = done;
    elements.activeCount.textContent = Math.max(state.todos.length - done, 0);
    elements.pageInfo.textContent = `Halaman ${page.page + 1} dari ${Math.max(page.totalPages, 1)}`;
    elements.prevPage.disabled = page.page === 0;
    elements.nextPage.disabled = page.last;

    if (state.todos.length === 0) {
        elements.todoList.innerHTML = `<div class="empty-state">Belum ada tugas di daftar ini.</div>`;
        return;
    }

    elements.todoList.innerHTML = state.todos.map((todo) => `
        <article class="todo-item ${todo.completed ? "done" : ""} ${isTodoOverdue(todo) ? "overdue" : ""}" data-id="${todo.id}">
            <button class="check-button" type="button" data-action="toggle" aria-label="Ubah status">
                ${todo.completed ? "OK" : ""}
            </button>
            <div>
                <div class="todo-title">${escapeHtml(todo.title)}</div>
                <div class="todo-meta">
                    <span>Dibuat ${formatDate(todo.createdAt)}</span>
                    ${todo.dueAt ? `<span class="alarm-pill">Alarm ${formatDate(todo.dueAt)}</span>` : ""}
                </div>
            </div>
            <div class="actions">
                <button class="icon-button" type="button" data-action="edit" aria-label="Edit">Edit</button>
                <button class="icon-button delete" type="button" data-action="delete" aria-label="Hapus">X</button>
            </div>
        </article>
    `).join("");
}

function escapeHtml(value) {
    return value.replace(/[&<>"']/g, (char) => ({
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        "\"": "&quot;",
        "'": "&#039;"
    })[char]);
}

function formatDate(value) {
    return new Intl.DateTimeFormat("id-ID", {
        dateStyle: "medium",
        timeStyle: "short"
    }).format(new Date(value));
}

function toInstantFromInput(value) {
    return value ? new Date(value).toISOString() : null;
}

function toInputDateTime(value) {
    if (!value) {
        return "";
    }
    const date = new Date(value);
    const offsetMs = date.getTimezoneOffset() * 60000;
    return new Date(date.getTime() - offsetMs).toISOString().slice(0, 16);
}

function isTodoOverdue(todo) {
    return Boolean(todo.dueAt && !todo.completed && new Date(todo.dueAt).getTime() <= Date.now());
}

async function createTodo(event) {
    event.preventDefault();
    const title = elements.todoTitle.value.trim();
    if (!title) {
        return;
    }

    try {
        await api("/api/todos", {
            method: "POST",
            body: JSON.stringify({
                title,
                completed: false,
                dueAt: toInstantFromInput(elements.todoDueAt.value)
            })
        });
        elements.todoTitle.value = "";
        elements.todoDueAt.value = "";
        state.page = 0;
        await loadTodos();
        setMessage(elements.todoMessage, "Todo berhasil ditambahkan.", "success");
    } catch (error) {
        setMessage(elements.todoMessage, error.message, "error");
    }
}

async function handleTodoAction(event) {
    const button = event.target.closest("button[data-action]");
    if (!button) {
        return;
    }

    const item = button.closest(".todo-item");
    const id = Number(item.dataset.id);
    const todo = state.todos.find((entry) => entry.id === id);
    if (!todo) {
        return;
    }

    try {
        if (button.dataset.action === "toggle") {
            await saveTodo(id, {
                title: todo.title,
                completed: !todo.completed,
                dueAt: todo.dueAt
            });
        }

        if (button.dataset.action === "edit") {
            openEditDialog(todo);
            return;
        }

        if (button.dataset.action === "delete") {
            await api(`/api/todos/${id}`, { method: "DELETE" });
        }

        await loadTodos();
    } catch (error) {
        setMessage(elements.todoMessage, error.message, "error");
    }
}

function openEditDialog(todo) {
    state.editingTodoId = todo.id;
    elements.editTitle.value = todo.title;
    elements.editDueAt.value = toInputDateTime(todo.dueAt);
    elements.editCompleted.checked = todo.completed;
    elements.editDialog.showModal();
}

async function submitEdit(event) {
    event.preventDefault();
    if (!state.editingTodoId) {
        return;
    }

    try {
        await saveTodo(state.editingTodoId, {
            title: elements.editTitle.value.trim(),
            completed: elements.editCompleted.checked,
            dueAt: toInstantFromInput(elements.editDueAt.value)
        });
        state.editingTodoId = null;
        elements.editDialog.close();
        await loadTodos();
        setMessage(elements.todoMessage, "Todo berhasil diperbarui.", "success");
    } catch (error) {
        setMessage(elements.todoMessage, error.message, "error");
    }
}

async function saveTodo(id, todo) {
    await api(`/api/todos/${id}`, {
        method: "PUT",
        body: JSON.stringify(todo)
    });
}

async function requestNotifications() {
    if (!("Notification" in window)) {
        setMessage(elements.todoMessage, "Browser ini belum mendukung notifikasi.", "error");
        return;
    }

    const permission = await Notification.requestPermission();
    updateNotificationButton();
    if (permission === "granted") {
        setMessage(elements.todoMessage, "Alarm browser aktif.", "success");
    } else {
        setMessage(elements.todoMessage, "Alarm tetap muncul di halaman, tapi notifikasi browser belum aktif.", "error");
    }
}

function updateNotificationButton() {
    if (!("Notification" in window)) {
        elements.notificationButton.textContent = "Alarm halaman";
        return;
    }
    elements.notificationButton.textContent = Notification.permission === "granted"
        ? "Alarm aktif"
        : "Aktifkan alarm";
}

async function checkAlarms() {
    if (!state.token) {
        return;
    }

    try {
        const page = await api("/api/todos?completed=false&page=0&size=50&sortBy=dueAt&direction=asc");
        const dueTodos = page.content.filter(isTodoOverdue);
        for (const todo of dueTodos) {
            const key = `${todo.id}:${todo.dueAt}`;
            if (state.notifiedAlarms[key]) {
                continue;
            }
            state.notifiedAlarms[key] = true;
            localStorage.setItem("todo_notified_alarms", JSON.stringify(state.notifiedAlarms));
            fireAlarm(todo);
        }
    } catch {
        // Alarm check should not disturb the main dashboard flow.
    }
}

function fireAlarm(todo) {
    playAlarmSound();
    setMessage(elements.todoMessage, `Alarm: ${todo.title}`, "error");

    if ("Notification" in window && Notification.permission === "granted") {
        new Notification("Todo alarm", {
            body: todo.title
        });
    } else {
        window.alert(`Alarm Todo: ${todo.title}`);
    }
}

function playAlarmSound() {
    const AudioContext = window.AudioContext || window.webkitAudioContext;
    if (!AudioContext) {
        return;
    }
    const audio = new AudioContext();
    const oscillator = audio.createOscillator();
    const gain = audio.createGain();
    oscillator.type = "sine";
    oscillator.frequency.value = 880;
    gain.gain.value = 0.12;
    oscillator.connect(gain);
    gain.connect(audio.destination);
    oscillator.start();
    oscillator.stop(audio.currentTime + 0.35);
}

let searchTimer;
function scheduleSearch() {
    window.clearTimeout(searchTimer);
    searchTimer = window.setTimeout(() => {
        state.page = 0;
        loadTodos();
    }, 250);
}

elements.loginTab.addEventListener("click", () => showLoginMode("login"));
elements.registerTab.addEventListener("click", () => showLoginMode("register"));
elements.loginForm.addEventListener("submit", login);
elements.registerForm.addEventListener("submit", register);
elements.notificationButton.addEventListener("click", requestNotifications);
elements.logoutButton.addEventListener("click", () => {
    clearSession();
    renderApp();
});
elements.todoForm.addEventListener("submit", createTodo);
elements.todoList.addEventListener("click", handleTodoAction);
elements.editForm.addEventListener("submit", submitEdit);
elements.cancelEdit.addEventListener("click", () => elements.editDialog.close());
elements.searchInput.addEventListener("input", scheduleSearch);
elements.filterSelect.addEventListener("change", () => {
    state.page = 0;
    loadTodos();
});
elements.sortSelect.addEventListener("change", () => {
    state.page = 0;
    loadTodos();
});
elements.prevPage.addEventListener("click", () => {
    state.page = Math.max(state.page - 1, 0);
    loadTodos();
});
elements.nextPage.addEventListener("click", () => {
    if (!state.last) {
        state.page += 1;
        loadTodos();
    }
});

window.setInterval(checkAlarms, 30000);

renderApp();
