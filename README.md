# Campus Wars

**Campus Wars** is a mobile *serious game* designed to promote social interaction and learning among university students.  
Players form teams and compete to conquer lecture halls across campus by winning subject-specific quizzes tied to the lectures currently taking place in each hall.

Through playful competition, the game encourages collaboration, academic engagement, and a stronger sense of community within the university environment.

---

## 🧩 Core Features
- **Team-based gameplay:** Students join teams and battle for control of campus buildings.  
- **Dynamic quiz generation:** Questions are tied to real lectures happening in each hall.  
- **Campus map integration:** Built with the **Google Maps API** for real-world navigation.  
- **Live data backend:** Game state updates dynamically based on current lecture schedules.  
- **Social engagement:** Encourages both cooperation and friendly rivalry among students.

---

## 🧠 Technical Overview
- **Frontend:** Android app built in **Java** using **Android Studio**  
- **Backend:** **Flask** (Python) REST API  
- **Database:** **MongoDB** for storing users, teams, and lecture data  
- **Integration:** Network analysis of the **campus.tum.de** platform to synchronize live lecture data  
- **Map Services:** Google Maps SDK for visualization and player positioning

---

## 🧪 Development Highlights
- Reverse-engineered the `campus.tum.de` traffic to understand real-time lecture scheduling and location updates.  
- Implemented a dynamic connection between the backend and in-game map to automatically reflect campus activity.  
- Designed the quiz logic to adapt difficulty and subjects based on live lecture metadata.



---

## 🛠️ Tech Stack
- **Languages:** Java, Python  
- **Frameworks:** Android Studio, Flask  
- **Database:** MongoDB  
- **APIs:** Google Maps  
- **Tools:** Postman, Wireshark (for traffic analysis)

---

## 👩‍💻 Authors
Developed by **Marina Weber** and collaborators as a university project at **TUM**.

---

## 📄 License
This project is provided for educational and non-commercial purposes.
