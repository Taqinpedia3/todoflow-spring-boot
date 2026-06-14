alter table todo add column due_at timestamp;

create index idx_todo_owner_due_at on todo(owner_id, due_at);
