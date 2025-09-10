# verify_face.py
import sys
import face_recognition

def compare_faces(registered_path, captured_path):
    try:
        reg_img = face_recognition.load_image_file(registered_path)
        cap_img = face_recognition.load_image_file(captured_path)

        reg_enc = face_recognition.face_encodings(reg_img)
        cap_enc = face_recognition.face_encodings(cap_img)

        if not reg_enc or not cap_enc:
            print("Error: No face found in one of the images")
            return

        result = face_recognition.compare_faces([reg_enc[0]], cap_enc[0])[0]
        print(result)
    except Exception as e:
        print(f"Error: {str(e)}")

if __name__ == "__main__":
    compare_faces(sys.argv[1], sys.argv[2])
