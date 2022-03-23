package com.viptech.game.ar;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.Face;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import com.viptech.game.vision.FaceAnalysis;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import android.widget.ImageView;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link CameraViewFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link CameraViewFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CameraViewFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private OnFragmentInteractionListener mListener;

    FaceAnalysis mFaceAnalysis = null;

    private final static String TAG = "SimpleCamera";
    private AutoFitTextureView mTextureView = null;
    private ImageView mInfoView = null;
    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            // TODO Auto-generated method stub
            Log.i(TAG, "onSurfaceTextureUpdated()");
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width,
                                                int height) {
            // TODO Auto-generated method stub
            Log.i(TAG, "onSurfaceTextureSizeChanged()");
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            // TODO Auto-generated method stub
            Log.i(TAG, "onSurfaceTextureDestroyed()");
            return false;
        }

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width,
                                              int height) {
            // TODO Auto-generated method stub
            Log.i(TAG, "onSurfaceTextureAvailable()");
            openCameraIfPossible(width, height);
        }
    };

    /*
        reference: https://inducesmile.com/android/android-camera2-api-example-tutorial/
        reference: https://developer.android.com/samples/Camera2Basic/index.html
     */
    private Size mPreviewSize = null;
    private CameraDevice mCameraDevice = null;
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);
    private Rect mActiveArraySize;
    private int mTargetFacing = CameraCharacteristics.LENS_FACING_FRONT;
    private int mCameraRotation = 90;
    //private Size mTargetSize = new Size(720, 480);
    private Size mTargetSize = new Size(1280, 960);

    //--------------------------------------------------------------------------------
    // reference: https://developer.android.com/training/permissions/requesting#java
    // reference: https://stackoverflow.com/questions/3423754/retrieving-android-api-version-programmatically
    // reference: https://github.com/mjohn123/Camera2APIM
    private int mPermissionsGranted = PackageManager.PERMISSION_DENIED;
    private static final int REQUEST_CODE_CAMERA = 333;

    private void openCameraIfPossible(int width, int height) {
        if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
            if (mPermissionsGranted == PackageManager.PERMISSION_GRANTED) {
                openCamera(width, height);
            }
            else
                Log.e(TAG, "No Permission, couldn't open camera.");
        }
        else { // old-typed permission granted
            openCamera(width, height);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CODE_CAMERA:
                mPermissionsGranted = PackageManager.PERMISSION_GRANTED;
                break;

            default:
                break;
        }
    }

    private void openCamera(int width, int height)
    {
        Log.i(TAG, ">> openCamera(" + width + "," + height + ")");
        final Activity activity = getActivity();
        final CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try{
/*
                String [] cameras = manager.getCameraIdList();
                for (int i = 0; i < cameras.length; i++)
                {
                    CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameras[i]);
                    StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    mPreviewSize = map.getOutputSizes(SurfaceTexture.class)[0];
                }*/

            Log.e(TAG, "=============================================================");
            for (String cameraID : manager.getCameraIdList()) {
                Log.e(TAG, "cameraID: "+ cameraID);
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraID);

                // Check Facing
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                Log.e(TAG, "facing: " + facing);

                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null) {
                    continue;
                }

                Size [] sizes = map.getOutputSizes(SurfaceTexture.class);
                for( Size size : sizes)
                {
                    Log.e(TAG, size.getWidth() + "x" + size.getHeight());
                }

                int sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                Log.e(TAG, "CameraOrientation: " + sensorOrientation);
            }
            Log.e(TAG, "=============================================================");

            for (String cameraID : manager.getCameraIdList()) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraID);

                // Check Facing
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing != mTargetFacing) {
                    continue;
                }

                if (mTargetFacing == CameraCharacteristics.LENS_FACING_BACK)
                {
                    mCameraRotation = 90;
                }
                else
                {
                    mCameraRotation = -90;
                }

                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null) {
                    continue;
                }


                int sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                if (sensorOrientation == 270)
                {

                }

                for (Size size : map.getOutputSizes(SurfaceTexture.class))
                {
                    if (size.getWidth() <= mTargetSize.getWidth() && size.getHeight() <= mTargetSize.getHeight()) {
                        mPreviewSize = size;
                        Log.e(TAG, "Setup " + mPreviewSize.getWidth() + "x" + mPreviewSize.getHeight());
                        break;
                    }
                }

                mActiveArraySize = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
                mTextureView.setAspectRatio(
                        mPreviewSize.getHeight(), mPreviewSize.getWidth());
                try {
                    if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                        throw new RuntimeException("Time out waiting to lock camera opening.");
                    }
                    manager.openCamera(cameraID, mStateCallback, null);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
                }
                break;
            }

        }
        catch(SecurityException e) {
            e.printStackTrace();
        }
        catch(CameraAccessException e) {
            e.printStackTrace();
        }
        finally {

        }

        Log.i(TAG, "<< openCamera()");
    }

    private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            if (null != mCaptureSession) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mImageReader) {
                mImageReader.close();
                mImageReader = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    private CaptureRequest.Builder mPreviewBuilder = null;
    private CameraCaptureSession mCaptureSession = null;
    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(CameraDevice camera) {
            // TODO Auto-generated method stub
            Log.i(TAG, "onOpened");
            mCameraOpenCloseLock.release();
            mCameraDevice = camera;

            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            if (texture == null) {
                Log.e(TAG, "texture is null");
                return;
            }

            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            mImageReader = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(), PixelFormat.RGBA_8888, 2);

            mImageReader.setOnImageAvailableListener(mReaderListener, mBackgroundHandler);

            Surface surface = new Surface(texture);

            try {
                mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            } catch (CameraAccessException e){
                e.printStackTrace();
            }

            mPreviewBuilder.addTarget(surface);
            mPreviewBuilder.addTarget(mImageReader.getSurface());

            List<Surface> outputSurfaces = new ArrayList<Surface>(2);
            outputSurfaces.add(surface);
            outputSurfaces.add(mImageReader.getSurface());

            try {
                mCameraDevice.createCaptureSession(outputSurfaces, mPreviewStateCallback, null);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            // TODO Auto-generated method stub
            Log.e(TAG, "onError");
            mCameraOpenCloseLock.release();
            closeCamera();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            // TODO Auto-generated method stub
            Log.e(TAG, "onDisconnected");
            mCameraOpenCloseLock.release();
            closeCamera();
        }
    };

    private CameraCaptureSession.StateCallback mPreviewStateCallback = new CameraCaptureSession.StateCallback() {

        @Override
        public void onConfigured(CameraCaptureSession session) {
            // TODO Auto-generated method stub
            Log.i(TAG, "onConfigured");
            mCaptureSession = session;

            mPreviewBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            mPreviewBuilder.set(CaptureRequest.STATISTICS_FACE_DETECT_MODE, CaptureRequest.STATISTICS_FACE_DETECT_MODE_SIMPLE);

            try {
                mCaptureSession.setRepeatingRequest(mPreviewBuilder.build(), mPreviewCaptureCallback, mBackgroundHandler);
                //mCaptureSession.setRepeatingRequest(mPreviewBuilder.build(), null, mBackgroundHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {
            // TODO Auto-generated method stub
            Log.e(TAG, "CameraCaptureSession Configure failed");
        }
    };

    Face[] mFaces = null;
    private CameraCaptureSession.CaptureCallback mPreviewCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request, CaptureResult partialResult) {
            super.onCaptureProgressed(session, request, partialResult);
        }

        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            result.getFrameNumber();
            Integer mode = result.get(CaptureResult.STATISTICS_FACE_DETECT_MODE);
            Face[] faces = result.get(CaptureResult.STATISTICS_FACES);
            if(faces != null && mode != null)
            {
                Log.e(TAG, "faces : " + faces.length + " , mode : " + mode );
                for (int i = 0; i < faces.length; i++)
                {
                    Rect face = faces[i].getBounds();
                    Log.e(TAG, "ActiveArraySize: (" + mActiveArraySize.left + "," + mActiveArraySize.top + "," + mActiveArraySize.width() + "," + mActiveArraySize.height() + ")");
                    Log.e(TAG, "face[" + i + "] (" + face.left + "," + face.top + "," + face.width() + ", " + face.height() +")" );
                }
            }
            mFaces = faces;
        }
    };

    private ImageReader mImageReader = null;
    private int align(int x)
    {
        return (x + 4 - (x%4));
    }

    private ImageReader.OnImageAvailableListener mReaderListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader imageReader) {
            Log.i(TAG, "onImageAvailable()");
            final int INPUT_SIZE = 256;

            Image image = imageReader.acquireLatestImage();
            if (image == null)
                return;

            try {

                if (mFaces != null && mFaces.length > 0 )
                {
                    Image.Plane plane = image.getPlanes()[0];
                    int bpp = 4;
                    ByteBuffer buffer = plane.getBuffer();
                    buffer.rewind();
                    int width = plane.getRowStride()/4;
                    int height = buffer.capacity()/plane.getRowStride();

                    Rect face = mFaces[0].getBounds();
                    int x = face.left*width/mActiveArraySize.width();
                    int y = face.top*height/mActiveArraySize.height();
                    int w = align(face.width()*width/mActiveArraySize.width());
                    int h = face.height()*height/mActiveArraySize.height();

                    x -= w*0.4;
                    y -= h*0.2;
                    w = (int) (w*1.8);
                    h = (int) (h*1.4);

                    // Check margin for face point detection
                    if (x >= 0 && y >= 0 && (x + w) < width && (y + h) < height)
                    {
                        Log.i(TAG, "(" + x + "," + y + "," + w + "," + h + ")");


                        //byte[] bytes = new byte[buffer.remaining()];
                        Log.i(TAG, "buffer.remaining():" + buffer.remaining());
                        Log.i(TAG, "buffer.capacity():" + buffer.capacity());
                        //buffer.get(bytes);

                        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                        Log.i(TAG, "Bitmap: " + bitmap.getWidth() +"x" + bitmap.getHeight());
                        bitmap.copyPixelsFromBuffer(buffer);

                        Bitmap face_bmp = Bitmap.createBitmap(bitmap, x, y, w, h);

                        Matrix matrix = new Matrix();
                        matrix.postRotate(mCameraRotation);
                        if (mTargetFacing == CameraCharacteristics.LENS_FACING_FRONT){
                            matrix.postScale(-1, 1, h/2, w/2);
                        }
                        final Bitmap face_bmp_rot = Bitmap.createBitmap(face_bmp, 0, 0, face_bmp.getWidth(), face_bmp.getHeight(), matrix, true);


                        int[] arr = mFaceAnalysis.detect(face_bmp_rot);

                        Canvas canvas = new Canvas(face_bmp_rot);
                        Paint paint = new Paint();
                        paint.setColor(Color.rgb(0, 255, 0));
                        paint.setStrokeWidth(10);
/*
                        for (int i = 0; i < arr.length/2; i++)
                        {
                            Point point = new Point(arr[2*i], arr[2*i + 1]);
                            //Log.e(TAG, "face_point[" + i + "]" + point.x + "," + point.y + ")");
                            //canvas.drawPoint(point.x, point.y, paint);
                        }
*/

                        getActivity().runOnUiThread(
                                new Runnable() {
                                    @Override
                                    public void run() {

                                        mInfoView.setImageBitmap(face_bmp_rot);
                                    }
                                }
                        );
                        //Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                        // Log.i(TAG, "test : arr[10] = " + arr[10]);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            finally {
                image.close();
            }
        }
    };

    private ImageReader.OnImageAvailableListener mReaderListener2 = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader imageReader) {
            Log.i(TAG, "onImageAvailable()");

            Log.e(TAG, "DETECT START\n");
            Image image = imageReader.acquireLatestImage();
            if (image == null)
                return;

            try {
                    Image.Plane plane = image.getPlanes()[0];
                    int bpp = 4;
                    ByteBuffer buffer = plane.getBuffer();
                    buffer.rewind();
                    int width = plane.getRowStride()/4;
                    int height = buffer.capacity()/plane.getRowStride();


                    //byte[] bytes = new byte[buffer.remaining()];
                    Log.i(TAG, "buffer.remaining():" + buffer.remaining());
                    Log.i(TAG, "buffer.capacity():" + buffer.capacity());
                    //buffer.get(bytes);

                    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                    Log.i(TAG, "Bitmap: " + bitmap.getWidth() +"x" + bitmap.getHeight());
                    bitmap.copyPixelsFromBuffer(buffer);

                    Matrix matrix = new Matrix();
                    matrix.postRotate(mCameraRotation);
                    final Bitmap bitmap_rot = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

                    int[] arr = mFaceAnalysis.detect(bitmap_rot);

                    Log.e(TAG, "DETECT DONE\n");
                    getActivity().runOnUiThread(
                        new Runnable() {
                            @Override
                            public void run() {

                                mInfoView.setImageBitmap(bitmap_rot);
                                Log.e(TAG, "COMPLETED\n");
                            }
                        }
                    );

            } catch (Exception e) {
                e.printStackTrace();
            }
            finally {
                image.close();
            }
        }
    };

    public CameraViewFragment() {
        // Required empty public constructor
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment CameraViewFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static CameraViewFragment newInstance(String param1, String param2) {
        CameraViewFragment fragment = new CameraViewFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mTextureView = (AutoFitTextureView) view.findViewById(R.id.cameraView);
        mInfoView = (ImageView) view.findViewById(R.id.infoView);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_camera_view, container, false);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

        this.mFaceAnalysis = new FaceAnalysis(getActivity().getAssets(), 1);

        //--------------------------------------------------------------------------------
        // reference: https://developer.android.com/training/permissions/requesting#java
        // reference: https://stackoverflow.com/questions/3423754/retrieving-android-api-version-programmatically
        // reference: https://github.com/mjohn123/Camera2APIM
        Log.i(TAG, "[AR:INFO] Device API Level: " + android.os.Build.VERSION.SDK_INT);

        if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
            mPermissionsGranted = ContextCompat.checkSelfPermission(this.getContext(), Manifest.permission.CAMERA);
            if (mPermissionsGranted == PackageManager.PERMISSION_DENIED) {
                ActivityCompat.requestPermissions(this.getActivity(), new String[]{Manifest.permission.CAMERA}, REQUEST_CODE_CAMERA);
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onResume() {
        Log.i(TAG, "onResume()");
        super.onResume();
        startBackgroundThread();

        if (mTextureView.isAvailable()) {
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    @Override
    public void onPause() {
        Log.i(TAG, "onPause()");
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }


}
