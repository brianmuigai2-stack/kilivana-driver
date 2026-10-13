package com.example.kilivana_driver.data.model

/**
 * Classifies a jobs-endpoint failure so the UI can show a
 * context-specific message instead of a generic error string.
 *
 * - [NO_INTERNET]  : the device has no network route (Wi-Fi / mobile data off,
 *                    DNS won't resolve, etc.). The backend might be fine; the
 *                    driver just can't reach it.
 * - [SERVER_DOWN]  : the backend host is unreachable, refused the connection,
 *                    timed out, or returned a 5xx. The server itself is the
 *                    problem.
 * - [OTHER]        : a 4xx (e.g. 404 / 403), a JSON parse error, or anything
 *                    else that doesn't fit the categories above.
 */
enum class JobsErrorType {
    NO_INTERNET,
    SERVER_DOWN,
    OTHER
}
