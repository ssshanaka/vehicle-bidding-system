**Project Overview**

The Web-based Vehicle Bidding System is an online auction platform developed for Lanka Auto Traders (Pvt) Ltd as part of the SE2030 Software Engineering course at the Sri Lanka Institute of Information Technology (SLIIT), Year 2, Semester 1, 2025. This system serves as a marketplace for second-hand and reconditioned vehicles, enabling public users to create, manage, and view bids. It acts as an online showroom where buyers can examine vehicle details and place bids, and sellers can list vehicles and connect with buyers post-auction via shared contact details (email/phone). The system includes user and admin roles for engagement and moderation, built using Java-based technologies and following Agile (Scrum) practices.

Objectives
* Develop a secure, user-friendly platform for vehicle bidding and listing management.
* Implement real-time bidding with countdown timers and transparent bid histories.
* Provide robust admin moderation tools to ensure platform integrity.
* Facilitate post-auction communication by sharing buyer-seller contact details.

Stakeholders
* Customers (Buyers/Sellers): Create listings, place bids, view bid histories, and access contact details.
* Admin Officers: Moderate listings, manage users, and ensure platform security.
* Sales Managers: Oversee auction schedules and statuses.
* Customer Service Executives: Handle inquiries and notify auction winners.
* Vehicle Inspectors: Verify listing details for accuracy.
* IT Consultants: Ensure system security and role-based access control.

Features
* User Management: Registration, login, role-based access (user/admin), and session security using JWT.
* Vehicle Listing Management: Create, edit, and delete listings with validated attributes (make, model, year, etc.).
* Bid Management: Place and validate bids, with real-time updates via WebSocket.
* Admin Moderation: Approve/reject listings, manage users, and monitor platform activity.
* Auction Management: Handle auction timers, statuses, and last-second bid extensions.
* Auction Reporting and Notifications: View bid histories, notify winners, share contact details, and generate reports.

Minor Features: 
* Password reset
* Search/filter listings
* Email notifications
* Profile management.

Non-Functional Requirements
* Security: HTTPS, hashed passwords, and role-based access control.
* Performance: Supports 100 concurrent users with 2-second bid updates.
* Usability: Intuitive, mobile-responsive UI using Bootstrap.
* Availability: 99.9% uptime.
* Scalability: Expandable to new vehicle types.

Tech Stack
* Backend: Java Spring Boot
* Database: MS SQL
* Frontend: JSP, CSS, JavaScript, Bootstrap
* Real-time Updates: WebSocket
* Authentication: JWT
* Development Methodology: Agile (Scrum) with 2-week sprints