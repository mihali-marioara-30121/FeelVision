[Mihali_Marioara_FeelVision.pdf](https://github.com/user-attachments/files/20160383/Mihali_Marioara_FeelVision.pdf)

# FeelVision

FeelVision is an Android application designed to assist visually impaired users by recognizing human emotions in real-time through facial analysis.
Using a Convolutional Neural Network (CNN), the app interprets facial expressions and provides accessible audio feedback via voice commands and Text-to-Speech technology.

## 🎯 Purpose

The main goal of FeelVision is to increase independence and social awareness for people with visual impairments by making non-verbal communication (emotions and expressions) more accessible through sound.

## 🧠 How It Works

1. The user points the phone camera toward a person.
2. The app captures the image and processes it using OpenCV.
3. A trained CNN model analyzes the facial expression and predicts the emotion.
4. The result is delivered to the user via voice output using Android's Text-to-Speech (TTS) system.
5. Users can control the app using voice commands.

## 🛠️ Tech Stack

### Machine Learning
- **Model**: Convolutional Neural Network (CNN)
- **Libraries**: TensorFlow, Keras
- **Training**: Performed in Python and exported for Android integration

### Image Processing
- **OpenCV** for real-time face detection and preprocessing

### Mobile Development
- **Android SDK**
- **Language**: Java

### Voice Interaction
- **Android Speech Recognition**: For voice commands
- **Text-to-Speech (TTS)**: For auditory feedback

## 📱 Key Features

- Emotion recognition: happy, neutral, surprised, etc.
- Real-time camera analysis
- Voice-controlled interaction
- Audio feedback via TTS
- Offline processing (no need for constant internet)

## 📷 Emotion Classes

The CNN model is trained to recognize the following 7 emotions:
- Happines
- Sadness
- Anger
- Surprise
- Neutral
- Fear
- Disgust
- Best results for: happines, neutral, surprise

## 🧪 Dataset

The CNN was trained using open-source facial expression dataset FER2013



