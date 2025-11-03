# 🏢 Project Description: Factory Inventory and Access Logging System

## 🔍 Overview
The **Factory Inventory and Access Logging System** is a **desktop application** developed using **JavaFX**, **SceneBuilder**, and **MySQL**. It is designed for **Tewodros Molla**, a factory owner who struggles with frequent product theft and poor tracking of goods.  

The system allows authorized personnel to **log product movements**, **track employee activity**, and **detect anomalies** (e.g., missing or delayed items). It replaces the client’s manual paper-based tracking with a digital, real-time logging and monitoring system that improves **security**, **efficiency**, and **accountability**.

---

## 🎯 Purpose
The main goal of the system is to **monitor and control the movement of products within the factory** to reduce theft and improve transparency.  

The system will:
1. Provide secure, role-based access for employees and managers.
2. Record every product check-in and check-out with timestamps.
3. Automatically detect suspicious or delayed product returns.
4. Generate daily reports summarizing all movements and alerts.
5. Maintain a clear audit trail of employee activity.

---

## 🧩 Functional Overview

| Feature | Description | User Roles Involved |
|----------|--------------|--------------------|
| **Login System** | Secure login for all employees using username and password. | All users |
| **Role Management** | Admin defines roles (e.g., Admin, Security, Staff). | Admin |
| **Product Logging** | Employees record when products leave or enter storage. | Staff |
| **Anomaly Detection** | System identifies products checked out for too long. | Automated |
| **Alert Dashboard** | Security or Admin can view unresolved alerts. | Admin, Security |
| **Daily Reports** | System generates a summary of all product movements and anomalies. | Admin |
| **Database Storage** | MySQL stores users, products, logs, and alerts. | System-wide |

---

## ⚙️ System Architecture
The system follows a **three-tier architecture**:

1. **Presentation Layer (JavaFX UI)**  
   Built with SceneBuilder to provide a user-friendly interface. It includes screens for:
   - Login  
   - Dashboard  
   - Product Log  
   - Alerts Page  
   - Admin Panel  

2. **Application Layer (Java Logic)**  
   Handles user interactions, data validation, and business logic:
   - Authentication and access control  
   - Product check-in/check-out operations  
   - Anomaly detection and alert generation  
   - Report generation and data analysis  

3. **Data Layer (MySQL Database)**  
   Stores all persistent information:
   - User credentials and roles  
   - Product inventory details  
   - Movement logs  
   - Alerts and report data  

---

## 🧠 How It Works (Process Flow)

1. **Login and Access Control**
   - Each employee logs in with credentials stored in the database.
   - The system grants access based on the assigned role:
     - *Staff*: can record product movements.  
     - *Security*: can view alerts.  
     - *Admin*: full system control (manage users, view reports).

2. **Product Movement Logging**
   - When an employee checks a product “out,” the system logs:
     - Product ID, user, action type, and timestamp.  
   - When the same product is checked “in,” the system updates its status.

3. **Anomaly Detection**
   - A background process monitors logs.
   - If a product remains “checked out” beyond a threshold (e.g., 2 hours), the system automatically creates an alert in the database.

4. **Alert and Report Generation**
   - Alerts appear on the dashboard in real-time.
   - Admins can mark alerts as resolved after investigation.
   - At the end of the day, a report summarizes all check-ins, check-outs, and alerts.

---

## 🧶 Database Design Summary

| Table | Key Purpose |
|--------|--------------|
| **users** | Stores login credentials and user roles. |
| **roles** | Defines access levels (Admin, Security, Staff). |
| **products** | Lists items stored or transported within the factory. |
| **logs** | Tracks product movements with timestamps and actions. |
| **alerts** | Records anomalies such as delayed or missing products. |

---

## 💡 Example Use Scenario

**Scenario:**  
A warehouse worker logs in and checks out 20 boxes of “Product A” for delivery. The system records the event.  
If those 20 boxes are not checked back in within the configured time, an **alert** is generated.  
Later, the security officer reviews the alert, confirms theft suspicion, and marks it as “resolved.”  
At day’s end, the manager prints a **daily report** summarizing total product movement and unresolved alerts.

---

## 🧮 Success Criteria (Measurable Outcomes)
1. The system authenticates users and restricts access by role.  
2. Product movements can be logged with 95% accuracy.  
3. Anomalies are detected and displayed on the dashboard within 5 seconds.  
4. Reports are generated automatically at the end of each day.  
5. Admin can manage users, roles, and alerts efficiently.

---

## 🧪 Tools & Technologies

| Component | Tool / Framework |
|------------|------------------|
| GUI Design | JavaFX with SceneBuilder |
| Programming Language | Java |
| Database | MySQL |
| JDBC Connector | MySQL Connector/J |
| Styling | JavaFX CSS |
| Evaluation Platform | Windows/Mac desktop environment |

---

## 🔒 Justification for Tools

- **JavaFX**: Allows visually interactive interfaces with minimal boilerplate code.  
- **SceneBuilder**: Simplifies GUI creation using drag-and-drop design.  
- **MySQL**: Provides a stable, structured database for multi-table relationships.  
- **JDBC**: Enables seamless Java–database integration for real-time data updates.  

---

## 🦯 Evaluation and Testing Plan

| Test Area | What Will Be Tested | Expected Outcome |
|------------|---------------------|------------------|
| Authentication | Invalid and valid login credentials | Only authorized users gain access |
| Product Logging | Check-in/check-out functions | Data correctly recorded in DB |
| Alert System | Simulated overdue items | Alert appears automatically |
| Report Generation | End-of-day report | Accurate and complete summary |
| Access Roles | Different user roles | Each role sees correct permissions |

---

## ✨ Expected Impact
By implementing this system, **Tewodros Molla** will gain:
- **Better security** through controlled access.  
- **Transparency** via real-time tracking.  
- **Reduced product loss** through early anomaly detection.  
- **Accountability** with detailed logs of employee actions.

