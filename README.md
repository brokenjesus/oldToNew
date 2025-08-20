# Импорт заметок из Старой системы в Новую систему

## Описание задачи

Приложение реализует автоматический импорт текстовых заметок из Старой системы в Новую систему.
Основные требования:

* Заметки импортируются по `guid` и синхронизируются с пациентами и пользователями в Новой системе.
* Дата создания заметки сохраняется из Старой системы.
* Сопоставление:
  * **Пациенты**: в Новой системе поле `patient_profile.old_client_guid` хранит `guid` клиентов из Старой системы.
  * **Пользователи**: идентифицируются по `login`.
* Импорт запускается по расписанию: **каждые два часа, в 15 минут первого часа**.
* Импортируются только активные пациенты (`status_id` ∈ {200, 210, 230}).
* При изменении заметок сохраняется наиболее поздняя версия.
* Реализовано логирование ошибок и статистики импорта.
* Старую систему имитирует REST API, доступный локально.

## Структура таблиц Новой системы

* **Пользователи (`company_user`)**
  `id, login`
* **Пациенты (`patient_profile`)**
  `id, first_name, last_name, old_client_guid, status_id`
* **Заметки (`patient_note`)**
  `id, created_date_time, last_modified_date_time, created_by_user_id, last_modified_by_user_id, note, patient_id`

## Конфигурация

Все параметры конфигурации задаются в `application.properties` или через переменные окружения.

### Переменные для подключения к БД

```properties
spring.datasource.url=jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=org.postgresql.Driver
```

| Переменная    | Описание        | Пример      |
| ------------- | --------------- | ----------- |
| `DB_HOST`     | Хост БД         | localhost   |
| `DB_PORT`     | Порт БД         | 5432        |
| `DB_NAME`     | Имя базы данных | new\_system |
| `DB_USERNAME` | Пользователь БД | postgres    |
| `DB_PASSWORD` | Пароль БД       | secret      |

### JPA / Hibernate

```properties
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.default_batch_fetch_size=16
```

### Liquibase

```properties
spring.liquibase.enabled=true
spring.liquibase.change-log=classpath:db/changelog/changelog-master.xml
```

### Настройки доступа к Старой системе

```properties
# URL REST API старой системы
app.old-system-base-url=${OLD_SYSTEM_URL}

# Количество лет для выборки заметок (например, последние 10 лет)
app.lookback-years=10
```

| Переменная       | Описание                            | Пример                                         |
| ---------------- | ----------------------------------- | ---------------------------------------------- |
| `OLD_SYSTEM_URL` | Базовый URL REST API старой системы | [http://localhost:8081](http://localhost:8081) |


## Запуск

1. Собрать образ:

   ```bash
   docker build -t old-to-new-importer .
   ```
2. Запустить контейнер:

   ```bash
   docker run -d \
     -e DB_HOST=localhost \
     -e DB_PORT=5432 \
     -e DB_NAME=new_system \
     -e DB_USERNAME=postgres \
     -e DB_PASSWORD=secret \
     -e OLD_SYSTEM_URL=http://old-system:8081 \
     --name importer \
     old-to-new-importer
   ```
