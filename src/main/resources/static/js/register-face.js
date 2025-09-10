// register-face.js
// Yêu cầu: đã include face-api.js và có thư mục /models chứa các model cần thiết

const video = document.getElementById("video");
const canvas = document.getElementById("overlay");
const resultDiv = document.getElementById("result");
const username = document.body.dataset.username;

// Trạng thái
let hasRegistered = false;      // đã gửi đăng ký (để tránh gửi trùng)
let isDetecting = false;        // khoá chồng lệnh detect song song
let detectTimer = null;         // interval handle

// Load model rồi bật camera
Promise.all([
    faceapi.nets.tinyFaceDetector.loadFromUri("/models"),
    faceapi.nets.faceLandmark68Net.loadFromUri("/models"),
    faceapi.nets.faceRecognitionNet.loadFromUri("/models"),
])
    .then(startVideo)
    .catch((err) => {
        console.error("Không thể load models:", err);
        showError("❌ Không thể tải mô-đun nhận diện khuôn mặt. Vui lòng tải lại trang.");
    });

// Mở camera
function startVideo() {
    // iOS cần playsinline để không bật full-screen
    video.setAttribute("playsinline", true);

    navigator.mediaDevices
        .getUserMedia({
            video: { facingMode: "user" }, // ưu tiên camera trước
            audio: false,
        })
        .then((stream) => {
            video.srcObject = stream;
            video.onloadedmetadata = () => {
                video.play().then(() => {
                    // Đồng bộ canvas theo độ phân giải thực của stream
                    syncCanvasSize();
                    startFaceDetection();
                });
            };
        })
        .catch((err) => {
            console.error("Không thể truy cập camera:", err);
            const msg =
                err && err.name === "NotAllowedError"
                    ? "❌ Truy cập camera bị từ chối. Vui lòng cấp quyền camera cho trình duyệt."
                    : "❌ Không thể truy cập camera.";
            showError(msg);
        });
}

// Dừng camera
function stopVideoStream() {
    const stream = video.srcObject;
    if (stream) {
        stream.getTracks().forEach((track) => track.stop());
        video.srcObject = null;
    }
}

// Đồng bộ kích thước canvas theo kích thước thực của video
function syncCanvasSize() {
    // video.videoWidth/videoHeight là kích thước thực của feed
    const w = video.videoWidth || video.width || 640;
    const h = video.videoHeight || video.height || 480;

    // Cập nhật thuộc tính width/height của <video> (tuỳ UI dự án)
    if (!video.width) video.width = w;
    if (!video.height) video.height = h;

    // Gán kích thước canvas để nét vẽ khớp pixel
    canvas.width = w;
    canvas.height = h;

    // Match dimensions cho face-api vẽ/scale đúng
    faceapi.matchDimensions(canvas, { width: w, height: h });
}

// Hiển thị thông điệp
function showSuccess(message) {
    resultDiv.textContent = message;
    resultDiv.className = "text-success fw-bold mt-3";
}
function showError(message) {
    resultDiv.textContent = message;
    resultDiv.className = "text-danger fw-bold mt-3";
}
function showInfo(message) {
    resultDiv.textContent = message;
    resultDiv.className = "text-primary fw-bold mt-3";
}

// Vẽ 1 vòng tròn quanh khuôn mặt
function drawCircle(box) {
    const ctx = canvas.getContext("2d");
    ctx.clearRect(0, 0, canvas.width, canvas.height);

    const { x, y, width, height } = box;
    const centerX = x + width / 2;
    const centerY = y + height / 2;
    const radius = Math.max(width, height) / 2;

    ctx.beginPath();
    ctx.arc(centerX, centerY, radius, 0, 2 * Math.PI);
    ctx.lineWidth = 4;
    ctx.strokeStyle = "#0d6efd";
    ctx.shadowColor = "#0d6efd";
    ctx.shadowBlur = 20;
    ctx.stroke();
    ctx.shadowBlur = 0;
}

// Vòng lặp phát hiện
function startFaceDetection() {
    // Nếu người dùng đổi kích cỡ cửa sổ hay video feed thay đổi tỉ lệ
    window.addEventListener("resize", syncCanvasSize);
    // Một số thiết bị cập nhật độ phân giải sau vài frame
    setTimeout(syncCanvasSize, 300);

    // Gợi ý trạng thái
    showInfo("📷 Đưa khuôn mặt vào khung để đăng ký…");

    // Dò mặt theo chu kỳ (nhẹ tài nguyên hơn rAF + đủ cho đăng ký)
    detectTimer = setInterval(async () => {
        if (hasRegistered || isDetecting) return;
        isDetecting = true;

        try {
            const detection = await faceapi
                .detectSingleFace(
                    video,
                    new faceapi.TinyFaceDetectorOptions({ inputSize: 320, scoreThreshold: 0.5 })
                )
                .withFaceLandmarks()
                .withFaceDescriptor();

            const ctx = canvas.getContext("2d");
            ctx.clearRect(0, 0, canvas.width, canvas.height);

            if (detection) {
                // Vẽ duy nhất 1 vòng tròn quanh khuôn mặt
                drawCircle(detection.detection.box);

                // Chốt & gửi đăng ký một lần
                hasRegistered = true;
                showInfo("📤 Đang lưu khuôn mặt…");

                const descriptor = Array.from(detection.descriptor);
                await submitDescriptor(descriptor);
            }
        } catch (err) {
            console.error("Lỗi detect:", err);
            // Không đổi trạng thái hasRegistered để có thể thử lại
        } finally {
            isDetecting = false;
        }
    }, 700);

    // Cleanup khi rời trang
    window.addEventListener("beforeunload", cleanup, { once: true });
}

// Gửi descriptor lên server
async function submitDescriptor(descriptor) {
    try {
        const res = await fetch("/api/register-descriptor", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ username, descriptor }),
        });

        // Cố gắng parse JSON (server có thể trả về JSON {message:...})
        let data = {};
        try {
            data = await res.json();
        } catch (_) {
            // ignore nếu không phải JSON
        }

        if (res.ok) {
            showSuccess("✅ Đăng ký thành công! Đang chuyển trang…");
            cleanup();
            setTimeout(() => {
                window.location.href = "/acvstore/verify-face";
            }, 1500);
            return;
        }

        // Không ok → hiển thị message server (ví dụ 409)
        const msg =
            (data && data.message) ||
            (res.status === 409
                ? "❌ Khuôn mặt này đã được đăng ký cho một tài khoản khác."
                : "❌ Không thể lưu khuôn mặt. Vui lòng thử lại.");
        showError(msg);

        // Cho phép thử lại (trừ khi bạn muốn chặn hẳn khi 409)
        hasRegistered = false;
    } catch (err) {
        console.error("Lỗi submit descriptor:", err);
        showError("❌ Không thể lưu khuôn mặt. Kiểm tra mạng và thử lại.");
        hasRegistered = false;
    }
}

// Dọn dẹp interval + camera
function cleanup() {
    if (detectTimer) {
        clearInterval(detectTimer);
        detectTimer = null;
    }
    stopVideoStream();
}
