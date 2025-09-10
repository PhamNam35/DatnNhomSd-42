document.addEventListener("DOMContentLoaded", async () => {
    // --- Khai báo các phần tử DOM và hằng số ---
    const video = document.getElementById("video");
    const resultDiv = document.getElementById("result");
    const canvas = document.getElementById("overlay");
    const statusIndicator = document.getElementById("status-indicator");
    const displaySize = { width: 600, height: 450 }; // Kích thước video gốc cho việc tải model và stream
    const username = document.body.getAttribute("data-username"); // Lấy username từ thuộc tính data của body
    const requiredMatches = 10; // Số lần khớp liên tiếp cần thiết để xác minh thành công
    const detectionInterval = 150; // Khoảng thời gian giữa các lần phát hiện (ms), ~6-7 FPS
    let lastDetectionTime = 0; // Biến theo dõi thời gian của lần phát hiện cuối cùng

    let faceMatcher = null; // Biến lưu trữ FaceMatcher sau khi tải descriptor

    console.log("🚀 Bắt đầu xác minh khuôn mặt cho:", username);

    // --- Hàm tiện ích: Cập nhật trạng thái hiển thị ---
    const updateStatus = (message, type = 'info') => {
        let icon = '';
        let color = '#cccccc'; // Màu xám mặc định

        switch (type) {
            case 'loading':
                icon = '<i class="fas fa-sync-alt fa-spin"></i>';
                color = '#3498db'; // Xanh dương
                break;
            case 'success':
                icon = '<i class="fas fa-check-circle"></i>';
                color = '#2ecc71'; // Xanh lá
                break;
            case 'error':
                icon = '<i class="fas fa-exclamation-circle"></i>';
                color = '#e74c3c'; // Đỏ
                break;
            case 'warning':
                icon = '<i class="fas fa-exclamation-triangle"></i>';
                color = '#f39c12'; // Cam
                break;
            case 'info':
            default:
                icon = '<i class="fas fa-info-circle"></i>';
                color = '#cccccc'; // Xám
                break;
        }
        statusIndicator.style.backgroundColor = color;
        resultDiv.innerHTML = `${icon} ${message}`;
    };

    // --- Hàm điều chỉnh kích thước Canvas để responsive ---
    const adjustCanvasSize = () => {
        const videoWrapper = video.parentElement;
        canvas.width = videoWrapper.clientWidth;
        canvas.height = videoWrapper.clientHeight;
        // Đảm bảo kích thước khớp với video để vẽ chính xác
        faceapi.matchDimensions(canvas, { width: videoWrapper.clientWidth, height: videoWrapper.clientHeight });
    };

    // --- Hàm tải các mô hình nhận diện khuôn mặt ---
    const loadFaceModels = async () => {
        updateStatus("Đang tải các mô hình nhận diện khuôn mặt...", 'loading');
        try {
            await Promise.all([
                faceapi.nets.tinyFaceDetector.loadFromUri("/models"),
                faceapi.nets.faceLandmark68Net.loadFromUri("/models"),
                faceapi.nets.faceRecognitionNet.loadFromUri("/models"),
            ]);
            console.log("✅ Các mô hình đã được tải thành công.");
        } catch (error) {
            console.error("❌ Lỗi khi tải mô hình:", error);
            throw new Error("Không thể tải mô hình nhận diện khuôn mặt. Vui lòng kiểm tra kết nối mạng.");
        }
    };

    // --- Hàm khởi động camera ---
    const startCamera = async () => {
        updateStatus("Đang mở camera của bạn...", 'loading');
        try {
            const stream = await navigator.mediaDevices.getUserMedia({
                video: {
                    width: displaySize.width,
                    height: displaySize.height
                }
            });
            video.srcObject = stream;
            // Trả về một Promise để đảm bảo video đã sẵn sàng phát
            return new Promise((resolve) => {
                video.onloadedmetadata = () => {
                    adjustCanvasSize(); // Điều chỉnh kích thước canvas sau khi video tải metadata
                    resolve(true);
                };
            });
        } catch (error) {
            console.error("❌ Lỗi khi truy cập camera:", error);
            if (error.name === "NotAllowedError") {
                throw new Error("Bạn đã từ chối quyền truy cập camera. Vui lòng cấp quyền trong cài đặt trình duyệt.");
            } else if (error.name === "NotFoundError") {
                throw new Error("Không tìm thấy camera. Vui lòng đảm bảo camera được kết nối và hoạt động.");
            } else {
                throw new Error(`Lỗi camera: ${error.message}.`);
            }
        }
    };

    // --- Hàm lấy dữ liệu descriptor khuôn mặt đã đăng ký từ backend ---
    const fetchUserDescriptors = async () => {
        updateStatus("Đang lấy dữ liệu khuôn mặt đã đăng ký...", 'loading');
        try {
            // **LƯU Ý QUAN TRỌNG VỀ BẢO MẬT:**
            // Trong dự án thực tế, API này PHẢI được bảo vệ bằng xác thực (ví dụ: JWT, Session).
            // `username` KHÔNG NÊN được truyền trực tiếp như một query parameter mà không có xác thực.
            // Chỉ người dùng đã đăng nhập MỚI được phép lấy descriptor của chính họ.
            const res = await fetch(`/api/get-descriptor?username=${encodeURIComponent(username)}`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                    // Thêm Authorization header nếu bạn dùng JWT hoặc các token khác
                    // 'Authorization': `Bearer ${yourAuthToken}`
                },
                credentials: 'include' // Bao gồm cookies/credentials nếu dùng session-based auth
            });

            if (!res.ok) {
                const errorData = await res.json();
                throw new Error(errorData.message || `Lỗi từ server: ${res.status} ${res.statusText}`);
            }

            const data = await res.json();

            if (!data.descriptors || !Array.isArray(data.descriptors) || data.descriptors.length === 0) {
                throw new Error("Không có dữ liệu khuôn mặt đã đăng ký cho tài khoản này. Vui lòng đăng ký khuôn mặt trước.");
            }

            // Chuyển đổi mảng các số thành Float32Array
            return new faceapi.LabeledFaceDescriptors(
                username,
                data.descriptors.map(d => new Float32Array(d))
            );
        } catch (error) {
            console.error("❌ Lỗi khi lấy dữ liệu khuôn mặt:", error);
            throw new Error(`Không thể lấy dữ liệu khuôn mặt đã đăng ký: ${error.message}.`);
        }
    };

    // --- Hàm chính: Vòng lặp phát hiện và so khớp khuôn mặt ---
    const runDetectionLoop = () => {
        let consecutiveMatches = 0; // Đếm số khung hình khớp liên tiếp
        const ctx = canvas.getContext("2d"); // Lấy context 2D của canvas

        const detect = async () => {
            // Giới hạn tần suất xử lý để tối ưu hiệu suất
            const currentTime = performance.now();
            if (currentTime - lastDetectionTime < detectionInterval) {
                requestAnimationFrame(detect);
                return;
            }
            lastDetectionTime = currentTime;

            // Phát hiện tất cả khuôn mặt trong video
            const detections = await faceapi
                .detectAllFaces(video, new faceapi.TinyFaceDetectorOptions())
                .withFaceLandmarks()
                .withFaceDescriptors();

            // Resize kết quả phát hiện để phù hợp với kích thước canvas hiện tại
            const resized = faceapi.resizeResults(detections, { width: canvas.width, height: canvas.height });
            ctx.clearRect(0, 0, canvas.width, canvas.height); // Xóa canvas trước khi vẽ khung mới

            if (resized.length === 0) {
                updateStatus("Không phát hiện khuôn mặt. Vui lòng nhìn vào camera.", 'warning');
                consecutiveMatches = 0; // Reset số lần khớp nếu không có khuôn mặt
                requestAnimationFrame(detect);
                return;
            }

            let currentFrameMatched = false; // Biến cờ cho biết khuôn mặt có khớp trong khung hình hiện tại không
            let bestMatchDistance = Infinity; // Khoảng cách nhỏ nhất của khuôn mặt khớp trong khung hình

            // Duyệt qua từng khuôn mặt được phát hiện
            resized.forEach((detection) => {
                const result = faceMatcher.findBestMatch(detection.descriptor); // Tìm khuôn mặt khớp nhất
                const box = detection.detection.box; // Lấy bounding box của khuôn mặt

                // Kiểm tra xem khuôn mặt có khớp với username và khoảng cách có đủ gần không
                // Ngưỡng 0.45 là một giá trị phổ biến, có thể điều chỉnh
                const isMatch = result.label === username && result.distance < 0.45;

                ctx.beginPath();
                ctx.lineWidth = 4;

                if (isMatch) {
                    currentFrameMatched = true;
                    bestMatchDistance = Math.min(bestMatchDistance, result.distance); // Cập nhật khoảng cách tốt nhất
                    ctx.strokeStyle = "rgba(0, 200, 83, 0.8)"; // Màu xanh lá cho khuôn mặt khớp
                    ctx.shadowColor = "rgba(0, 200, 83, 0.8)";
                    ctx.shadowBlur = 15;
                } else {
                    ctx.strokeStyle = "rgba(255, 69, 58, 0.6)"; // Màu đỏ cam cho khuôn mặt không khớp
                    ctx.shadowColor = "rgba(255, 69, 58, 0.8)";
                    ctx.shadowBlur = 10;
                }

                // Vẽ hình elip để tạo khung cho khuôn mặt
                ctx.ellipse(
                    box.x + box.width / 2,
                    box.y + box.height / 2,
                    box.width / 2,
                    box.height / 2,
                    0,
                    0,
                    2 * Math.PI
                );
                ctx.stroke();
                ctx.shadowBlur = 0; // Tắt shadow sau khi vẽ khung
                ctx.shadowColor = "transparent";
            });

            // Cập nhật số lần khớp liên tiếp và trạng thái
            if (currentFrameMatched) {
                consecutiveMatches++;
                updateStatus(`Đã khớp khuôn mặt (${consecutiveMatches}/${requiredMatches}). Khoảng cách: ${bestMatchDistance.toFixed(2)}`, 'success');
            } else {
                consecutiveMatches = 0;
                updateStatus("Khuôn mặt không khớp hoặc không hợp lệ. Đang chờ khuôn mặt hợp lệ...", 'warning');
            }

            // Nếu đạt đủ số lần khớp liên tiếp
            if (consecutiveMatches >= requiredMatches) {
                updateStatus("Xác minh thành công! Đang chuyển hướng...", 'success');
                // Dừng stream camera để giải phóng tài nguyên và bảo mật
                if (video.srcObject) {
                    video.srcObject.getTracks().forEach(track => track.stop());
                }

                // Gửi thông báo xác minh thành công về backend
                try {
                    // **LƯU Ý QUAN TRỌNG VỀ BẢO MẬT:**
                    // API này cũng PHẢI được bảo vệ bằng xác thực (JWT/Session) để ngăn chặn
                    // kẻ tấn công giả mạo yêu cầu xác minh thành công.
                    const response = await fetch("/acvstore/verify-success", {
                        method: "POST",
                        headers: { "Content-Type": "application/json" },
                        body: JSON.stringify({ username }),
                        credentials: "include" // Bao gồm cookies/credentials
                    });
                    const data = await response.json();

                    if (data.success) {
                        setTimeout(() => {
                            window.location.href = "/acvstore/thong-ke"; // Chuyển hướng sau 1.5 giây
                        }, 1500);
                    } else {
                        // Nếu backend báo lỗi, hiển thị lỗi và cho phép thử lại
                        updateStatus(`Xác minh thất bại: ${data.message || "Lỗi không xác định khi ghi nhận thành công."}`, 'error');
                        consecutiveMatches = 0; // Reset để người dùng có thể thử lại
                        requestAnimationFrame(detect); // Tiếp tục vòng lặp
                    }
                } catch (err) {
                    console.error("Lỗi khi gửi xác minh thành công về backend:", err);
                    updateStatus("Lỗi kết nối: Không thể gửi xác minh thành công. Vui lòng thử lại.", 'error');
                    consecutiveMatches = 0; // Reset để người dùng có thể thử lại
                    requestAnimationFrame(detect); // Tiếp tục vòng lặp
                }
                return; // Ngừng vòng lặp phát hiện nếu đã xác minh thành công
            }

            requestAnimationFrame(detect); // Tiếp tục vòng lặp phát hiện
        };

        detect(); // Bắt đầu vòng lặp phát hiện
    };

    // --- Logic khởi tạo chính của ứng dụng ---
    const initializeFaceVerification = async () => {
        try {
            await loadFaceModels(); // 1. Tải các mô hình Face-API
            await startCamera();    // 2. Khởi động camera và chờ video sẵn sàng

            // Chỉ chạy các bước sau khi video đã bắt đầu phát
            video.addEventListener("play", async () => {
                try {
                    const labeledDescriptor = await fetchUserDescriptors(); // 3. Lấy descriptor khuôn mặt
                    faceMatcher = new faceapi.FaceMatcher([labeledDescriptor], 0.45); // 4. Khởi tạo FaceMatcher
                    runDetectionLoop(); // 5. Bắt đầu vòng lặp nhận diện và so khớp
                } catch (error) {
                    console.error("❌ Lỗi trong quá trình sau khi camera khởi động:", error);
                    updateStatus(`Không thể bắt đầu xác minh: ${error.message}`, 'error');
                    // Dừng camera nếu có lỗi trong quá trình khởi tạo sau khi camera bật
                    if (video.srcObject) {
                        video.srcObject.getTracks().forEach(track => track.stop());
                    }
                }
            }, { once: true }); // Đảm bảo event listener này chỉ chạy một lần

        } catch (error) {
            // Xử lý lỗi trong quá trình khởi tạo ban đầu (tải model hoặc bật camera)
            console.error("❌ Lỗi khởi tạo hệ thống:", error);
            updateStatus(`Lỗi nghiêm trọng: ${error.message}. Vui lòng tải lại trang.`, 'error');
            // Dừng camera nếu có lỗi ngay khi khởi tạo
            if (video.srcObject) {
                video.srcObject.getTracks().forEach(track => track.stop());
            }
        }
    };

    // --- Bắt đầu quá trình xác minh khi DOM đã tải ---
    initializeFaceVerification();

    // --- Đảm bảo canvas responsive khi cửa sổ thay đổi kích thước ---
    window.addEventListener('resize', adjustCanvasSize);
});