#!/usr/bin/env python3
"""Generates Material-style vector drawables and shape drawables for the app."""
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DRAW = os.path.join(ROOT, "app", "src", "main", "res", "drawable")
os.makedirs(DRAW, exist_ok=True)

VECTOR = """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24"
    android:tint="?attr/colorControlNormal">
    <path
        android:fillColor="@android:color/white"
        android:pathData="{path}" />
</vector>
"""

# name -> (material path, autoMirrored)
ICONS = {
    "ic_add": ("M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z", False),
    "ic_remove": ("M19 13H5v-2h14v2z", False),
    "ic_close": ("M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z", False),
    "ic_check": ("M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z", False),
    "ic_check_circle": ("M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z", False),
    "ic_cancel": ("M12 2C6.47 2 2 6.47 2 12s4.47 10 10 10 10-4.47 10-10S17.53 2 12 2zm5 13.59L15.59 17 12 13.41 8.41 17 7 15.59 10.59 12 7 8.41 8.41 7 12 10.59 15.59 7 17 8.41 13.41 12 17 15.59z", False),
    "ic_search": ("M15.5 14h-.79l-.28-.27a6.5 6.5 0 1 0-.7.7l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0A4.5 4.5 0 1 1 14 9.5 4.5 4.5 0 0 1 9.5 14z", False),
    "ic_refresh": ("M17.65 6.35A7.958 7.958 0 0 0 12 4a8 8 0 1 0 7.73 10h-2.08A6 6 0 1 1 12 6c1.66 0 3.14.69 4.22 1.8L13 11h7V4l-2.35 2.35z", False),
    "ic_delete": ("M6 19a2 2 0 0 0 2 2h8a2 2 0 0 0 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z", False),
    "ic_edit": ("M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04a.996.996 0 0 0 0-1.41l-2.34-2.34a.996.996 0 0 0-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z", False),
    "ic_more_vert": ("M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z", False),
    "ic_logout": ("M17 7l-1.41 1.41L18.17 11H8v2h10.17l-2.58 2.58L17 17l5-5zM4 5h8V3H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h8v-2H4V5z", True),
    "ic_login": ("M11 7L9.6 8.4l2.6 2.6H2v2h10.2l-2.6 2.6L11 17l5-5-5-5zm9 12h-8v2h8c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2h-8v2h8v14z", True),
    "ic_share": ("M18 16.08c-.76 0-1.44.3-1.96.77L8.91 12.7c.05-.23.09-.46.09-.7s-.04-.47-.09-.7l7.05-4.11c.54.5 1.25.81 2.04.81 1.66 0 3-1.34 3-3s-1.34-3-3-3-3 1.34-3 3c0 .24.04.47.09.7L8.04 9.81C7.5 9.31 6.79 9 6 9c-1.66 0-3 1.34-3 3s1.34 3 3 3c.79 0 1.5-.31 2.04-.81l7.12 4.16c-.05.21-.08.43-.08.65 0 1.61 1.31 2.92 2.92 2.92s2.92-1.31 2.92-2.92-1.31-2.92-2.92-2.92z", False),
    "ic_copy": ("M16 1H4c-1.1 0-2 .9-2 2v14h2V3h12V1zm3 4H8c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 16H8V7h11v14z", False),
    "ic_visibility": ("M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17a5 5 0 1 1 0-10 5 5 0 0 1 0 10zm0-8a3 3 0 1 0 0 6 3 3 0 0 0 0-6z", False),
    "ic_visibility_off": ("M12 7c2.76 0 5 2.24 5 5 0 .65-.13 1.26-.36 1.83l2.92 2.92c1.51-1.26 2.7-2.89 3.43-4.75-1.73-4.39-6-7.5-11-7.5-1.4 0-2.74.25-3.98.7l2.16 2.16C10.74 7.13 11.35 7 12 7zM2 4.27l2.28 2.28.46.46A11.804 11.804 0 0 0 1 12c1.73 4.39 6 7.5 11 7.5 1.55 0 3.03-.3 4.38-.84l.42.42L19.73 22 21 20.73 3.27 3 2 4.27zM7.53 9.8l1.55 1.55A2.996 2.996 0 0 1 12 15c.22 0 .44-.03.65-.08l1.6 1.6c-.67.31-1.4.48-2.25.48a5.006 5.006 0 0 1-5-5c0-.85.23-1.65.63-2.34l-.1-.16z", False),
    "ic_info": ("M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z", False),
    "ic_warning": ("M1 21h22L12 2 1 21zm12-3h-2v-2h2v2zm0-4h-2v-4h2v4z", False),
    "ic_bolt": ("M11 21h-1l1-7H7.5c-.58 0-.57-.32-.38-.66.19-.34.05-.08.07-.12C8.48 10.94 10.42 7.54 13 3h1l-1 7h3.5c.49 0 .56.33.47.51l-.07.15C14.96 15.03 12.96 18.88 12 18z", False),
    "ic_database": ("M12 3C7.58 3 4 4.79 4 7s3.58 4 8 4 8-1.79 8-4-3.58-4-8-4zM4 9v3c0 2.21 3.58 4 8 4s8-1.79 8-4V9c0 2.21-3.58 4-8 4S4 11.21 4 9zm0 5v3c0 2.21 3.58 4 8 4s8-1.79 8-4v-3c0 2.21-3.58 4-8 4s-8-1.79-8-4z", False),
    "ic_people": ("M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z", False),
    "ic_person": ("M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z", False),
    "ic_person_add": ("M15 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm-9-2V7H4v3H1v2h3v3h2v-3h3v-2H6zm9 4c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z", False),
    "ic_shield": ("M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4zm-2 16l-4-4 1.41-1.41L10 14.17l6.59-6.59L18 9l-8 8z", False),
    "ic_dns": ("M20,13H4c-0.55,0 -1,0.45 -1,1v6c0,0.55 0.45,1 1,1h16c0.55,0 1,-0.45 1,-1v-6c0,-0.55 -0.45,-1 -1,-1zM7,19H5v-2h2v2zm0,-4H5v-2h2v2zM20,3H4c-0.55,0 -1,0.45 -1,1v6c0,0.55 0.45,1 1,1h16c0.55,0 1,-0.45 1,-1V4c0,-0.55 -0.45,-1 -1,-1zM7,9H5V7h2v2zm0,-4H5V3h2v2z", False),
    "ic_globe": ("M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zm6.93 6h-2.95a15.65 15.65 0 0 0-1.38-3.56A8.03 8.03 0 0 1 18.92 8zM12 4.04c.83 1.2 1.48 2.53 1.91 3.96h-3.82c.43-1.43 1.08-2.76 1.91-3.96zM4.26 14C4.1 13.36 4 12.69 4 12s.1-1.36.26-2h3.38c-.08.66-.14 1.32-.14 2 0 .68.06 1.34.14 2H4.26zm.82 2h2.95c.32 1.25.78 2.45 1.38 3.56A7.987 7.987 0 0 1 5.08 16zm2.95-8H5.08a7.987 7.987 0 0 1 4.33-3.56C8.81 5.55 8.35 6.75 8.03 8zM12 19.96c-.83-1.2-1.48-2.53-1.91-3.96h3.82c-.43 1.43-1.08 2.76-1.91 3.96zM14.34 14H9.66c-.09-.66-.16-1.32-.16-2 0-.68.07-1.35.16-2h4.68c.09.65.16 1.32.16 2 0 .68-.07 1.34-.16 2zm.25 5.56c.6-1.11 1.06-2.31 1.38-3.56h2.95a8.03 8.03 0 0 1-4.33 3.56zM16.36 14c.08-.66.14-1.32.14-2 0-.68-.06-1.34-.14-2h3.38c.16.64.26 1.31.26 2s-.1 1.36-.26 2h-3.38z", False),
    "ic_swap": ("M6.99 11L3 15l3.99 4v-3H14v-2H6.99v-3zM21 9l-3.99-4v3H10v2h7.01v3L21 9z", False),
    "ic_vpn_key": ("M12.65 10A5.99 5.99 0 0 0 7 6c-3.31 0-6 2.69-6 6s2.69 6 6 6a5.99 5.99 0 0 0 5.65-4H17v4h4v-4h2v-4H12.65zM7 15c-1.65 0-3-1.35-3-3s1.35-3 3-3 3 1.35 3 3-1.35 3-3 3z", False),
    "ic_settings": ("M19.14 12.94c.04-.3.06-.61.06-.94 0-.32-.02-.64-.07-.94l2.03-1.58a.49.49 0 0 0 .12-.61l-1.92-3.32a.488.488 0 0 0-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94l-.36-2.54a.484.484 0 0 0-.48-.41h-3.84c-.24 0-.43.17-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96c-.22-.08-.47 0-.59.22L2.74 8.87c-.12.21-.08.47.12.61l2.03 1.58c-.05.3-.09.63-.09.94s.02.64.07.94l-2.03 1.58a.49.49 0 0 0-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32c.12-.22.07-.47-.12-.61l-2.01-1.58zM12 15.6c-1.98 0-3.6-1.62-3.6-3.6s1.62-3.6 3.6-3.6 3.6 1.62 3.6 3.6-1.62 3.6-3.6 3.6z", False),
    "ic_cloud": ("M19.35 10.04A7.49 7.49 0 0 0 12 4C9.11 4 6.6 5.64 5.35 8.04A5.994 5.994 0 0 0 0 14c0 3.31 2.69 6 6 6h13c2.76 0 5-2.24 5-5 0-2.64-2.05-4.78-4.65-4.96z", False),
    "ic_article": ("M20 2H4c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm-9 14H6v-2h5v2zm6-4H6v-2h11v2zm0-4H6V6h11v2z", False),
    "ic_campaign": ("M18 11v2h4v-2h-4zm-2 6.61c.96.71 2.21 1.65 3.2 2.39l.8-1.2c-1-.8-2.24-1.68-3.2-2.4l-.8 1.21zm4-13.61l-.8-1.2c-.99.74-2.24 1.69-3.2 2.4l.8 1.2c.96-.72 2.2-1.6 3.2-2.4zM4 9c-1.1 0-2 .9-2 2v2c0 1.1.9 2 2 2h1v4h2v-4h1l5 3V6L8 9H4zm11.5 3c0-1.33-.58-2.53-1.5-3.35v6.69c.92-.81 1.5-2.01 1.5-3.34z", False),
    "ic_clean": ("M16 11h-1V3c0-1.1-.9-2-2-2h-2c-1.1 0-2 .9-2 2v8H8c-2.76 0-5 2.24-5 5v7h18v-7c0-2.76-2.24-5-5-5zm-5-8h2v8h-2V3zm8 18H5v-2c0-1.66 1.34-3 3-3h8c1.66 0 3 1.34 3 3v2z", False),
    "ic_tag": ("M21.41 11.58l-9-9C12.05 2.22 11.55 2 11 2H4c-1.1 0-2 .9-2 2v7c0 .55.22 1.05.59 1.42l9 9c.36.36.86.58 1.41.58.55 0 1.05-.22 1.41-.59l7-7c.37-.36.59-.86.59-1.41 0-.55-.23-1.06-.59-1.42zM5.5 7C4.67 7 4 6.33 4 5.5S4.67 4 5.5 4 7 4.67 7 5.5 6.33 7 5.5 7z", False),
    "ic_layers": ("M11.99 18.54L4.62 12.81 3 14.07l9 7 9-7-1.63-1.27-8.38 5.74zm.01-2.54l7.36-5.73L21 9l-9-7-9 7 1.63 1.27L12 16zm0-11.47L17.74 9 12 13.47 6.26 9 12 4.53z", False),
    "ic_dashboard": ("M3 13h8V3H3v10zm0 8h8v-6H3v6zm10 0h8V11h-8v10zm0-18v6h8V3h-8z", False),
    "ic_download": ("M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z", False),
    "ic_upload": ("M9 16h6v-6h4l-7-7-7 7h4v6zm-4 2h14v2H5v-2z", False),
    "ic_qr": ("M3 11h8V3H3v8zm2-6h4v4H5V5zm8-2v8h8V3h-8zm6 6h-4V5h4v4zM3 21h8v-8H3v8zm2-6h4v4H5v-4zm13-2h3v3h-2v-3h-1zm-3 3h2v2h-2zm3 3h2v2h-2zm-3 0h2v2h-2v-2z", False),
    "ic_link": ("M3.9 17.3a.996.996 0 0 0 0 1.41l1.41 1.41c.39.39 1.02.39 1.41 0l3.77-3.77c.39-.39.39-1.02 0-1.41l-1.41-1.41a.996.996 0 0 0-1.41 0l-1.06 1.06-2.47-2.47 1.06-1.06a.996.996 0 0 0 0-1.41l-1.41-1.41a.996.996 0 0 0-1.41 0L2.38 9.5a.996.996 0 0 0 0 1.41l1.52 1.52-2.47 2.47 2.47 2.4zm4.24-4.24l2.47-2.47 6.16-6.16c.39-.39 1.02-.39 1.41 0l1.41 1.41c.39.39.39 1.02 0 1.41l-6.16 6.16-2.47 2.47-2.82-2.82zm12.76-10.9l-1.41-1.41a.996.996 0 0 0-1.41 0l-1.06 1.06 2.83 2.83 1.06-1.06c.38-.38.38-1.02-.01-1.42z", False),
    "ic_chevron_right": ("M10 6L8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6z", True),
    "ic_chevron_down": ("M16.59 8.59L12 13.17 7.41 8.59 6 10l6 6 6-6z", False),
    "ic_arrow_back": ("M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z", True),
    "ic_filter": ("M10 18h4v-2h-4v2zM3 6v2h18V6H3zm3 7h12v-2H6v2z", False),
    "ic_lock": ("M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z", False),
    "ic_lock_open": ("M12 17c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm6-9h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6h1.9c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm0 12H6V10h12v10z", False),
    "ic_memory": ("M15 9H9v6h6V9zm-2 4h-2v-2h2v2zm8-2V9h-2V7c0-1.1-.9-2-2-2h-2V3h-2v2h-2V3H9v2H7c-1.1 0-2 .9-2 2v2H3v2h2v2H3v2h2v2c0 1.1.9 2 2 2h2v2h2v-2h2v2h2v-2h2c1.1 0 2-.9 2-2v-2h2v-2h-2v-2h2zm-4 6H7V7h10v10z", False),
    "ic_storage": ("M2 20h20v-4H2v4zm2-3h2v2H4v-2zM2 4v4h20V4H2zm4 3H4V5h2v2zm-4 7h20v-4H2v4zm2-3h2v2H4v-2z", False),
    "ic_speed": ("M20.38 8.57l-1.23 1.85a8 8 0 0 1-.22 7.58H5.07A8 8 0 0 1 15.58 6.85l1.85-1.23A10 10 0 0 0 3.35 19a2 2 0 0 0 1.72 1h13.85a2 2 0 0 0 1.74-1 10 10 0 0 0-.27-10.44zm-9.79 6.84a2 2 0 0 0 2.83 0l5.66-8.49-8.49 5.66a2 2 0 0 0 0 2.83z", False),
    "ic_play": ("M8 5v14l11-7z", False),
    "ic_stop": ("M6 6h12v12H6z", False),
    "ic_restart": ("M12 5V1L7 6l5 5V7a5 5 0 1 1-5 5H5a7 7 0 1 0 7-7z", False),
    "ic_terminal": ("M4 18h16v-2H4v2zm0-7l4 4-1.41 1.41L1.17 13 6.59 7.59 8 9l-4 2zm16-2H10v2h10V9z", False),
    "ic_tune": ("M3 17v2h6v-2H3zM3 5v2h10V5H3zm10 16v-2h8v-2h-8v-2h-2v6h2zM7 9v2H3v2h4v2h2V9H7zm14 4v-2H11v2h10zm-6-4h2V7h4V5h-4V3h-2v6z", False),
    "ic_send": ("M2.01 21L23 12 2.01 3 2 10l15 2-15 2z", False),
    "ic_wifi": ("M1 9l2 2c4.97-4.97 13.03-4.97 18 0l2-2C16.93 2.93 7.08 2.93 1 9zm8 8l3 3 3-3a4.237 4.237 0 0 0-6 0zm-4-4l2 2a7.074 7.074 0 0 1 10 0l2-2C15.14 9.14 8.87 9.14 5 13z", False),
    "ic_schedule": ("M13 3a9 9 0 1 0 9 9h-2a7 7 0 1 1-7-7V3zm-1 5v5l4.28 2.54.72-1.21-3.5-2.08V8H12z", False),
    "ic_save": ("M17 3H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14c1.1 0 2-.9 2-2V7l-4-4zm-5 16c-1.66 0-3-1.34-3-3s1.34-3 3-3 3 1.34 3 3-1.34 3-3 3zm3-10H5V5h10v4z", False),
    "ic_drag": ("M20 9H4v2h16V9zM4 15h16v-2H4v2z", False),
    "ic_pin": ("M16 12V4h1V2H7v2h1v8l-2 2v2h5.2v6h1.6v-6H18v-2l-2-2z", False),
    "ic_apps": ("M4 8h4V4H4v4zm6 12h4v-4h-4v4zm-6 0h4v-4H4v4zm0-6h4v-4H4v4zm6 0h4v-4h-4v4zm6-10v4h4V4h-4zm-6 4h4V4h-4v4zm6 6h4v-4h-4v4zm0 6h4v-4h-4v4z", False),
    "ic_brightness": ("M8 9h3V6h2v3h3v2h-3v3h-2v-3H8V9zm4-7a10 10 0 1 0 0 20 10 10 0 0 0 0-20z", False),
    "ic_star": ("M12 17.27L18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z", False),
    "ic_calendar": ("M19 4h-1V2h-2v2H8V2H6v2H5c-1.11 0-1.99.9-1.99 2L3 20a2 2 0 0 0 2 2h14c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 16H5V10h14v10zM5 8V6h14v2H5z", False),
    "ic_sun": ("M6.76 4.84l-1.8-1.79-1.41 1.41 1.79 1.79 1.42-1.41zM4 10.5H1v2h3v-2zm9-9.95h-2V3.5h2V.55zm7.45 3.91l-1.41-1.41-1.79 1.79 1.41 1.41 1.79-1.79zm-3.21 13.7l1.79 1.8 1.41-1.41-1.8-1.79-1.4 1.4zM20 10.5v2h3v-2h-3zm-8-5c-3.31 0-6 2.69-6 6s2.69 6 6 6 6-2.69 6-6-2.69-6-6-6zm-1 16.95h2V19.5h-2v2.95zm-7.45-3.91l1.41 1.41 1.79-1.8-1.41-1.41-1.79 1.8z", False),
    "ic_chart": ("M3.5 18.49l6-6.01 4 4L22 6.92l-1.41-1.41-7.09 7.97-4-4L2 16.99z", False),
    "ic_pie": ("M11 2v20C5.93 21.5 2 17.27 2 12S5.93 2.5 11 2zm2 0v9h9c-.47-4.69-4.31-8.53-9-9zm0 11v9c4.69-.47 8.53-4.31 9-9h-9z", False),
    "ic_toggle_on": ("M17 7H7c-2.76 0-5 2.24-5 5s2.24 5 5 5h10c2.76 0 5-2.24 5-5s-2.24-5-5-5zm0 8c-1.65 0-3-1.35-3-3s1.35-3 3-3 3 1.35 3 3-1.35 3-3 3z", False),
    "ic_router": ("M20.2 5.9l.8-3.4-3.4.8C16.17 2.5 14.21 2 12 2 6.5 2 2 6.5 2 12s4.5 10 10 10 10-4.5 10-10c0-2.21-.5-4.17-1.3-5.6L20.2 5.9zM12 20c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm1-10h-2v2H9v2h2v2h2v-2h2v-2h-2v-2z", False),
    "ic_code": ("M9.4 16.6L4.8 12l4.6-4.6L8 6l-6 6 6 6 1.4-1.4zm5.2 0l4.6-4.6-4.6-4.6L16 6l6 6-6 6-1.4-1.4z", False),
    "ic_block": ("M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zM4 12c0-4.42 3.58-8 8-8 1.85 0 3.55.63 4.9 1.69L5.69 16.9A7.902 7.902 0 0 1 4 12zm8 8c-1.85 0-3.55-.63-4.9-1.69L18.31 7.1A7.902 7.902 0 0 1 20 12c0 4.42-3.58 8-8 8z", False),
    "ic_monitor": ("M21 16V4H3v12h18zm0-14c1.1 0 2 .9 2 2v14c0 1.1-.9 2-2 2h-7v2h-4v-2H3c-1.1 0-2-.9-2-2V4c0-1.1.9-2 2-2h18z", False),
    "ic_folder": ("M10 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2h-8l-2-2z", False),
    "ic_unfold": ("M12 5.83L15.17 9l1.41-1.41L12 3 7.41 7.59 8.83 9 12 5.83zm0 12.34L8.83 15l-1.41 1.41L12 21l4.59-4.59L15.17 15 12 18.17z", False),
    "ic_attach": ("M16.5 6v11.5c0 2.21-1.79 4-4 4s-4-1.79-4-4V5a2.5 2.5 0 0 1 5 0v10.5c0 .55-.45 1-1 1s-1-.45-1-1V6H10v9.5a2.5 2.5 0 0 0 5 0V5c0-2.21-1.79-4-4-4S7 2.79 7 5v12.5c0 3.04 2.46 5.5 5.5 5.5s5.5-2.46 5.5-5.5V6h-1.5z", False),
    "ic_sort": ("M3 18h6v-2H3v2zM3 6v2h18V6H3zm0 7h12v-2H3v2z", False),
    "ic_playlist_add": ("M3 10h11v2H3v-2zm0-4h11v2H3V6zm0 8h7v2H3v-2zm13 0v4h2v-4h4v-2h-4v-4h-2v4h-4v2h4z", False),
    "ic_account_tree": ("M22 11V3h-7v3H9V3H2v8h7V8h2v10h4v3h7v-8h-7v3h-2V8h2v3h7zM7 9H4V5h3v4zm10 6h3v4h-3v-4zm0-10h3v4h-3V5z", False),
    "ic_list": ("M4 10.5c-.83 0-1.5.67-1.5 1.5s.67 1.5 1.5 1.5 1.5-.67 1.5-1.5-.67-1.5-1.5-1.5zm0-6c-.83 0-1.5.67-1.5 1.5S3.17 7.5 4 7.5 5.5 6.83 5.5 6 4.83 4.5 4 4.5zm0 12c-.83 0-1.5.68-1.5 1.5s.68 1.5 1.5 1.5 1.5-.67 1.5-1.5-.67-1.5-1.5-1.5zM7 19h14v-2H7v2zm0-6h14v-2H7v2zm0-8v2h14V5H7z", False),
    "ic_inbox": ("M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 12h-4c0 1.66-1.34 3-3 3s-3-1.34-3-3H5V5h14v10z", False),
    "ic_bug": ("M20 8h-2.81a5.985 5.985 0 0 0-1.82-1.96L17 4.41 15.59 3l-2.17 2.17C12.96 5.06 12.49 5 12 5c-.49 0-.96.06-1.41.17L8.41 3 7 4.41l1.62 1.63C7.88 7.63 7.26 9.26 7.26 11c0 .34.03.67.08 1H4v2h2.81A5.99 5.99 0 0 0 8.54 17L7 18.59 8.41 20l1.76-1.76c.55.23 1.15.38 1.83.42V22h2v-3.34c.68-.04 1.28-.19 1.83-.42L17.59 20 19 18.59 17.46 17c.93-.93 1.57-2.2 1.73-3.61V13H22v-2h-3.34c.05-.33.08-.66.08-1 0-.34-.03-.67-.08-1H20V8zm-6 8c-1.65 0-3-1.35-3-3s1.35-3 3-3 3 1.35 3 3-1.35 3-3 3z", False),
    "ic_open_new": ("M19 19H5V5h7V3H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14c1.1 0 2-.9 2-2v-7h-2v7zM14 3v2h3.59l-9.83 9.83 1.41 1.41L19 6.41V10h2V3h-7z", False),
    "ic_devices": ("M4 6h18V4H4c-1.1 0-2 .9-2 2v11H0v3h14v-3H4V6zm19 2h-6c-.55 0-1 .45-1 1v10c0 .55.45 1 1 1h6c.55 0 1-.45 1-1V9c0-.55-.45-1-1-1zm-1 9h-4v-7h4v7z", False),
    "ic_power": ("M13 3h-2v10h2V3zm4.83 2.17l-1.42 1.42A6.92 6.92 0 0 1 19 12c0 3.87-3.13 7-7 7A6.995 6.995 0 0 1 7.58 6.58L6.17 5.17A8.932 8.932 0 0 0 3 12a9 9 0 0 0 18 0c0-2.74-1.23-5.18-3.17-6.83z", False),
    "ic_cloud_off": ("M19.35 10.04A7.49 7.49 0 0 0 12 4c-1.48 0-2.85.43-4.01 1.17l1.46 1.46C10.21 6.23 11.06 6 12 6c3.04 0 5.5 2.46 5.5 5.5v.5H19c1.66 0 3 1.34 3 3 0 1.13-.64 2.11-1.56 2.62l1.55 1.55A4.96 4.96 0 0 0 24 15c0-2.64-2.05-4.78-4.65-4.96zM3 5.27l2.75 2.74C2.56 8.15 0 10.77 0 14c0 3.31 2.69 6 6 6h11.73l2 2L21 20.73 4.27 4 3 5.27zM7.73 10l8 8H6c-2.21 0-4-1.79-4-4s1.79-4 4-4h1.73z", False),
    "ic_menu": ("M3 18h18v-2H3v2zm0-5h18v-2H3v2zm0-7v2h18V6H3z", False),
    "ic_notifications": ("M12 22c1.1 0 2-.9 2-2h-4a2 2 0 0 0 2 2zm6-6v-5c0-3.07-1.64-5.64-4.5-6.32V4c0-.83-.67-1.5-1.5-1.5S10.5 3.17 10.5 4v.68C7.63 5.36 6 7.92 6 11v5l-2 2v1h16v-1l-2-2z", False),
    "ic_signal": ("M17 4h3v16h-3V4zM5 14h3v6H5v-6zm6-5h3v11h-3V9z", False),
    "ic_translate": ("M12.87,15.07l-2.54-2.51.03-.03A17.52 17.52 0 0 0 14.07 6H17V4h-7V2H8v2H1v1.99h11.17C11.5 7.92 10.44 9.75 9 11.35 8.07 10.32 7.3 9.19 6.69 8h-2c.73 1.63 1.73 3.17 2.98 4.56l-5.09 5.02L4 19l5-5 3.11 3.11.76-2.04zM18.5 10h-2L12 22h2l1.12-3h4.75L21 22h2l-4.5-12zm-2.62 7l1.62-4.33L19.12 17h-3.24z", False),
    "ic_system_update": ("M17 16h-4v3l3.59-3.59L15.17 14 11 18.17V15H7v-2h10v3zm5-4c0-1.1-.9-2-2-2H4c-1.1 0-2 .9-2 2v8c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2v-8zM12 3.5L15.5 7 17 5.59l-5-5-5 5L8.5 7 12 3.5z", False),
    "ic_fact_check": ("M20 3H4c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 16H4V5h16v14zM19.41 6.59L17.59 4.76 10.59 11.76 8.41 9.59 6.59 11.41l4 4 8.82-8.82z", False),
}

for name, (path, mirrored) in ICONS.items():
    xml = VECTOR.format(path=path)
    if mirrored:
        xml = xml.replace("<vector ", '<vector android:autoMirrored="true" ', 1)
    with open(os.path.join(DRAW, name + ".xml"), "w", encoding="utf-8") as f:
        f.write(xml)

# ---- Shape drawables -------------------------------------------------------
SHAPES = {
    "bg_card.xml": """<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="?attr/colorSurface" />
    <corners android:radius="20dp" />
    <stroke android:width="1dp" android:color="?attr/colorOutlineVariant" />
</shape>
""",
    "bg_card_brand.xml": """<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <gradient android:angle="135" android:startColor="@color/brand_600" android:endColor="@color/brand_400" />
    <corners android:radius="20dp" />
</shape>
""",
    "bg_chip.xml": """<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="?attr/colorSurfaceVariant" />
    <corners android:radius="12dp" />
</shape>
""",
    "bg_chip_brand.xml": """<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="?attr/colorPrimaryContainer" />
    <corners android:radius="12dp" />
</shape>
""",
    "bg_text_field.xml": """<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="?attr/colorSurfaceVariant" />
    <corners android:radius="14dp" />
</shape>
""",
    "bg_dashboard_header.xml": """<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <gradient android:angle="135" android:startColor="@color/brand_700" android:endColor="@color/brand_500" />
    <corners android:bottomLeftRadius="28dp" android:bottomRightRadius="28dp" />
</shape>
""",
    "bg_icon_circle.xml": """<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="oval">
    <solid android:color="?attr/colorPrimaryContainer" />
</shape>
""",
    "bg_splash.xml": """<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <gradient android:angle="135" android:startColor="@color/brand_700" android:endColor="@color/brand_400" />
</shape>
""",
    "bg_login_pattern.xml": """<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item>
        <shape android:shape="rectangle">
            <gradient android:angle="135" android:startColor="@color/brand_800" android:endColor="@color/brand_500" />
        </shape>
    </item>
    <item android:gravity="top|end" android:width="220dp" android:height="220dp" android:right="-60dp" android:top="-60dp">
        <shape android:shape="oval">
            <solid android:color="#22FFFFFF" />
        </shape>
    </item>
    <item android:gravity="bottom|start" android:width="260dp" android:height="260dp" android:left="-90dp" android:bottom="-90dp">
        <shape android:shape="oval">
            <solid android:color="#1F102060" />
        </shape>
    </item>
</layer-list>
""",
}

STATUS_DOTS = {
    "bg_dot_active.xml": "@color/status_active",
    "bg_dot_disabled.xml": "@color/status_disabled",
    "bg_dot_expired.xml": "@color/status_expired",
    "bg_dot_no_data.xml": "@color/status_no_data",
    "bg_dot_online.xml": "@color/status_online",
    "bg_dot_offline.xml": "@color/status_offline",
    "bg_dot_late.xml": "@color/status_late",
    "bg_dot_never.xml": "@color/status_disabled",
}
for fname, color in STATUS_DOTS.items():
    SHAPES[fname] = f"""<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="oval">
    <solid android:color="{color}" />
    <size android:width="10dp" android:height="10dp" />
</shape>
"""

for fname, xml in SHAPES.items():
    with open(os.path.join(DRAW, fname), "w", encoding="utf-8") as f:
        f.write(xml)

# Splash logo: white Hiddify mark bitmap, inset into the splash safe zone.
with open(os.path.join(DRAW, "ic_splash_logo.xml"), "w", encoding="utf-8") as f:
    f.write("""<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:width="96dp" android:height="96dp" android:gravity="center"
        android:drawable="@drawable/hiddify_logo_white" />
</layer-list>
""")

print(f"Generated {len(ICONS)} vector icons and {len(SHAPES) + 1} shape drawables in {DRAW}")
