### how to run (local without docker)

1. open the project in **IntelliJ** or **VS Code (with Java plugin)**

2. make sure you have **MySQL running locally**

   * db name: `apartment_db`
   * username: `root`
   * password: `admin123`

3. open `src/main/resources/application.yaml` (or `.properties`)
   and check that the datasource matches your local db

   ```yaml
   spring:
     datasource:
       url: jdbc:mysql://localhost:3306/apartment_db?useSSL=false&serverTimezone=UTC
       username: root
       password: admin123
   ```

4. build and run the app

   ```bash
   ./gradlew bootRun
   ```

   or if you’re using IntelliJ — just press **Run ▶**

---

### how to run with docker-compose

1. go to the root of the project (where `docker-compose.yml` is)

2. build and start everything

   ```bash
   docker-compose build
   docker-compose up -d
   ```

   will start:

   * MySQL on port **3306**
   * Backend on **[http://localhost:8080](http://localhost:8080)**
   * (Frontend on **[http://localhost:3000](http://localhost:3000)**, if you added it)

3. check logs

   ```bash
   docker logs -f my-backend-app
   ```

---

### API base

once its up you can test the login:

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}'
```

if it returns a token you’re good to go

then frontend should connect automatically using that same port

---

### folder overview

```
src/
  main/java/com/devsop/apartmentinvoice/
    controller/
    entity/
    repository/
    service/
  resources/
    application.yaml
Dockerfile
docker-compose.yml
```

---

### notes

* dont push your `.env` (if you have one)
* its okay to push `docker-compose.yml` — it helps others run the same setup
* jwt secret, db password, etc -> move to env variable if it’s sensitive
* backend listens on port `8080`, db on `3306`

---

### useful gradle commands

```bash
./gradlew clean build      # build jar
./gradlew bootRun          # run directly
./gradlew test             # run tests
```

---

### ✅ quick check list

* [ ] db is up (check with `docker ps` or MySQL client)
* [ ] backend running on port 8080
* [ ] you can login from frontend
* [ ] authorization headers show up in Network tab

---
