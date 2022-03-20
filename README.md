# ZMD Project

- This project is showcasing the JPEG compression/decompression tools via a Java gui
- To run this project run `MainWindow.java` file in `IntelliJ`
- Possible sampling methods:
    - `4:2:2`
    - `4:2:0`
    - `4:1:1`
- Possible 2D transformations:
    - `2D-DCT` -- Discrete cosine transformation
    - `2D-WHT` -- Discrete Walsh-Hadamard transformation
- Possible measurements:
    - `MSE` -- Mean squared error
    - `PSNR` -- Peak signal-to-noise ratio

- This project also showcases watermarking techniques which include:
    - Spacial watermarking of a black and white image
    - Frequency watermarking of a black and white image
- This project also includes attack methods to these watermarking techniques such as:
    - Attack with a JPEG compression
    - Attack by rotating 45 and 90 degrees