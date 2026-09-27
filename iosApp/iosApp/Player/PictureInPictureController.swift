import AVFoundation
import AVKit
import UIKit

/// Drives a Picture in Picture window for the mpv-backed player.
///
/// mpv is embedded via the "wid" option, meaning it owns the CAMetalLayer's swapchain directly
/// (through MoltenVK) and renders straight to it - there's no per-frame texture the app can hand
/// to `AVSampleBufferDisplayLayer` for real, live PiP video without a deeper change to how mpv is
/// embedded. Instead, this periodically grabs a still frame via mpv's `screenshot-to-file`
/// command (a separate, self-contained mpv feature that doesn't touch the render pipeline) and
/// feeds that into the PiP window. It's a live-updating thumbnail rather than smooth video -
/// good enough to show "something is still playing" while backgrounded, not frame-perfect.
@available(iOS 15.0, *)
final class MPVPictureInPictureController: NSObject, AVPictureInPictureSampleBufferPlaybackDelegate {

    private static let captureIntervalSeconds: TimeInterval = 1.0

    private weak var owner: MPVPlayerViewController?
    private let sampleBufferDisplayLayer = AVSampleBufferDisplayLayer()
    private var pipController: AVPictureInPictureController?
    private var captureTimer: Timer?
    private let screenshotURL: URL
    private var isCapturing = false

    init(owner: MPVPlayerViewController) {
        self.owner = owner
        self.screenshotURL = FileManager.default.temporaryDirectory
            .appendingPathComponent("nuvio_pip_frame_\(ProcessInfo.processInfo.globallyUniqueString).png")
        super.init()

        sampleBufferDisplayLayer.videoGravity = .resizeAspect
        let contentSource = AVPictureInPictureController.ContentSource(
            sampleBufferDisplayLayer: sampleBufferDisplayLayer,
            playbackDelegate: self
        )
        let controller = AVPictureInPictureController(contentSource: contentSource)
        controller.requiresLinearPlayback = false
        pipController = controller
    }

    deinit {
        try? FileManager.default.removeItem(at: screenshotURL)
    }

    var isSupported: Bool {
        AVPictureInPictureController.isPictureInPictureSupported()
    }

    @discardableResult
    func start() -> Bool {
        guard isSupported, let pipController, !pipController.isPictureInPictureActive else { return false }
        beginCapturing()
        pipController.startPictureInPicture()
        return true
    }

    func stop() {
        pipController?.stopPictureInPicture()
        stopCapturing()
    }

    // MARK: - Frame capture

    private func beginCapturing() {
        guard !isCapturing else { return }
        isCapturing = true
        captureFrame()
        let timer = Timer(timeInterval: Self.captureIntervalSeconds, repeats: true) { [weak self] _ in
            self?.captureFrame()
        }
        RunLoop.main.add(timer, forMode: .common)
        captureTimer = timer
    }

    private func stopCapturing() {
        isCapturing = false
        captureTimer?.invalidate()
        captureTimer = nil
    }

    private func captureFrame() {
        guard isCapturing, let owner else { return }
        owner.captureCurrentFrame(to: screenshotURL) { [weak self] success in
            guard success, let self, self.isCapturing else { return }
            guard
                let image = UIImage(contentsOfFile: self.screenshotURL.path),
                let cgImage = image.cgImage
            else { return }
            self.enqueue(cgImage: cgImage)
        }
    }

    private func enqueue(cgImage: CGImage) {
        var pixelBufferOrNil: CVPixelBuffer?
        let attrs: [CFString: Any] = [
            kCVPixelBufferCGImageCompatibilityKey: true,
            kCVPixelBufferCGBitmapContextCompatibilityKey: true,
        ]
        let createStatus = CVPixelBufferCreate(
            kCFAllocatorDefault,
            cgImage.width,
            cgImage.height,
            kCVPixelFormatType_32BGRA,
            attrs as CFDictionary,
            &pixelBufferOrNil
        )
        guard createStatus == kCVReturnSuccess, let pixelBuffer = pixelBufferOrNil else { return }

        CVPixelBufferLockBaseAddress(pixelBuffer, [])
        let drawSucceeded: Bool = {
            guard let context = CGContext(
                data: CVPixelBufferGetBaseAddress(pixelBuffer),
                width: cgImage.width,
                height: cgImage.height,
                bitsPerComponent: 8,
                bytesPerRow: CVPixelBufferGetBytesPerRow(pixelBuffer),
                space: CGColorSpaceCreateDeviceRGB(),
                bitmapInfo: CGImageAlphaInfo.noneSkipFirst.rawValue | CGBitmapInfo.byteOrder32Little.rawValue
            ) else { return false }
            context.draw(cgImage, in: CGRect(x: 0, y: 0, width: cgImage.width, height: cgImage.height))
            return true
        }()
        CVPixelBufferUnlockBaseAddress(pixelBuffer, [])
        guard drawSucceeded else { return }

        var formatDescription: CMVideoFormatDescription?
        CMVideoFormatDescriptionCreateForImageBuffer(
            allocator: kCFAllocatorDefault,
            imageBuffer: pixelBuffer,
            formatDescriptionOut: &formatDescription
        )
        guard let formatDescription else { return }

        var timingInfo = CMSampleTimingInfo(
            duration: CMTime(seconds: Self.captureIntervalSeconds, preferredTimescale: 600),
            presentationTimeStamp: CMClockGetTime(CMClockGetHostTimeClock()),
            decodeTimeStamp: .invalid
        )
        var sampleBufferOrNil: CMSampleBuffer?
        CMSampleBufferCreateForImageBuffer(
            allocator: kCFAllocatorDefault,
            imageBuffer: pixelBuffer,
            dataReady: true,
            makeDataReadyCallback: nil,
            refcon: nil,
            formatDescription: formatDescription,
            sampleTiming: &timingInfo,
            sampleBufferOut: &sampleBufferOrNil
        )
        guard let sampleBuffer = sampleBufferOrNil else { return }

        if sampleBufferDisplayLayer.status == .failed {
            sampleBufferDisplayLayer.flush()
        }
        sampleBufferDisplayLayer.enqueue(sampleBuffer)
    }

    // MARK: - AVPictureInPictureSampleBufferPlaybackDelegate

    func pictureInPictureController(
        _ pictureInPictureController: AVPictureInPictureController,
        setPlaying playing: Bool
    ) {
        if playing {
            owner?.resumeForPictureInPicture()
        } else {
            owner?.pauseForPictureInPicture()
        }
    }

    func pictureInPictureControllerTimeRangeForPlayback(
        _ pictureInPictureController: AVPictureInPictureController
    ) -> CMTimeRange {
        guard let owner, owner.durationMs > 0 else {
            return CMTimeRange(start: .zero, duration: .positiveInfinity)
        }
        let durationSeconds = Double(owner.durationMs) / 1000.0
        return CMTimeRange(start: .zero, duration: CMTime(seconds: durationSeconds, preferredTimescale: 600))
    }

    func pictureInPictureControllerIsPlaybackPaused(
        _ pictureInPictureController: AVPictureInPictureController
    ) -> Bool {
        !(owner?.isPlayerPlaying ?? false)
    }

    func pictureInPictureController(
        _ pictureInPictureController: AVPictureInPictureController,
        didTransitionToRenderSize newRenderSize: CMVideoDimensions
    ) {
    }

    func pictureInPictureController(
        _ pictureInPictureController: AVPictureInPictureController,
        skipByInterval skipInterval: CMTime,
        completion completionHandler: @escaping () -> Void
    ) {
        owner?.seekForPictureInPicture(byMs: Int64(skipInterval.seconds * 1000))
        completionHandler()
    }
}
