# Send Node - BST Platform Features Documentation

This document outlines the full suite of backend features, frontend interaction modules, and UI/UX design characteristics implemented in the Send Node Binary Search Tree visual learning platform.

---

## 1. Backend & Data Persistence

The backend relies on lightweight file-based data stores coupled with robust security and access controls:

- **Lightweight Properties Database**: 
  - Automatically resolves base path directories (next to the JAR or fallbacks to the working directory) to read/write `users.properties` and `progress.properties` files.
- **Secure Password Hashing**:
  - Uses `SHA-256` hashing to protect student and teacher credentials.
  - Implements automatic password hashing upgrades on first login for legacy plain-text accounts.
- **Role-Based Access Control (RBAC)**:
  - Dynamically routes users at login based on their role:
    - **Student**: Redirects to the Student Dashboard and Visualizer.
    - **Teacher**: Redirects to the Teacher Portal and Analytics Dashboard.
- **Challenge Progress Engine**:
  - Tracks solved problems by unique ID, enabling linear progression, unlocking checkmarks, and computing user experience (XP).
- **Proctoring / Violation Auditing**:
  - Persists focus violation counts directly to the database. These violations can be reset by instructors.
- **Database Management Interface**:
  - Support for resetting all data/violations for a specific student (`resetProgressForUser`).
  - Support for deleting student accounts entirely (`deleteUser`).

---

## 2. Frontend & Visualizer Interactive Features

The user interface integrates code editing, visual simulation, and progress tracking:

- **Multi-Tree Visualizer Panel**:
  - Fully implements four tree representations on an interactive infinite canvas:
    - **Binary Search Tree (BST)**
    - **AVL Tree** (Self-balancing via left/right rotations)
    - **Red-Black Tree** (Recoloring, double-red violation checks, rotation steps)
    - **B-Tree / 2-3 Tree** (Leaf-aligned layout, split propagation, key merging, and underflow borrowing/merging)
- **Java Code Simulator (Execution Engine)**:
  - Mock Java compiler that parses constructor definitions, method calls (`insert`, `delete`, `search`, `clear`), output statements (`System.out.println`), array declarations, and loops.
  - **Batch Execution (`Run All`)**: Parses and executes code sequentially with delay buffers to illustrate changes.
  - **Step-by-Step Debugger (`Step`)**: Highlights and steps through lines one-by-one.
  - **Comment Stripping**: Automatically detects and strips single-line (`//`) and multi-line (`/* */`) comments before execution.
- **Manual Operations Console**:
  - Visual insertion, deletion, and searching fields.
  - Interactive Random Tree generator and Clear Tree operations.
- **Canvas Panning & Zooming**:
  - Supports canvas dragging to pan and scroll wheel interactions to zoom on large node architectures.
- **Proctoring / Focus-Lock System**:
  - Monitors OS window-focus and minimize events.
  - Displays a full-screen warning overlay with animated warning counters when window focus is lost.
  - Disables middle-clicks, right-clicks, print-screens, and screenshot shortcuts (`Ctrl+P`, `Win+Shift+S`) to prevent content copying.
- **Teacher Analytics Portal**:
  - **XP Leaderboard**: Interactive Bar Chart tracking top student scores.
  - **Difficulty Distribution**: Pie Chart displaying solve distribution across levels.
  - **Concept Completion Rates**: Bar Chart displaying student solve frequency per problem ID.
  - **Focus Violations Tracker**: Bar Chart logging student focus violations, containing a visual overlay if clean.
  - **Interactive Search**: Search bar to filter student listings.
  - **Detailed Progress Panel**: Displays student Rank, XP, Violations status, and a detailed checkmark grid of all 20 challenges.

---

## 3. UI/UX Design & Typography

The design features custom CSS, animations, and typography:

- **Curated Glassmorphism Design System**:
  - Uses deep slate colors (`Theme.BG = "#0f172a"`), dark navy surfaces, and semi-transparent panels.
  - Includes custom drop shadows and neon glows on borders.
- **Visual Branding and Graphics**:
  - Animated BST graphics on the Welcome Screen with floating translate transitions.
  - Custom image loading with chroma-key transparency filters to match gradients.
- **Dynamic Accent Signaling**:
  - Sky Teal (`#0ea5e9`) for visualizer elements, Emerald Green (`#10b981`) for successes, Amber Orange (`#f59e0b`) for warnings, and Red (`#ef4444`) for locks or violations.
- **Responsive Window Layouts**:
  - Implements dynamic split panes and scrollable containers to fit windows >= 900x580.
- **Diagonal Security Watermarks**:
  - Mouse-transparent username and protected status stamps overlaid across Challenge views to prevent clean screenshots.
- **Staggered Animations**:
  - Cross-fading scene navigations, path sliding pointers with gold hop highlights, bouncing cards, and node zoom scaling.
