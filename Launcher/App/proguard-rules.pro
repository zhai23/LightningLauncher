# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /home/luki/Android/Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
-dontwarn java.beans.BeanInfo
-dontwarn java.beans.FeatureDescriptor
-dontwarn java.beans.IntrospectionException
-dontwarn java.beans.Introspector
-dontwarn java.beans.PropertyDescriptor
-dontwarn com.google.android.gms.fido.Fido
-dontwarn com.google.android.gms.fido.common.Transport
-dontwarn com.google.android.gms.fido.fido2.Fido2PrivilegedApiClient
-dontwarn com.google.android.gms.fido.fido2.api.common.Algorithm
-dontwarn com.google.android.gms.fido.fido2.api.common.Attachment
-dontwarn com.google.android.gms.fido.fido2.api.common.AttestationConveyancePreference
-dontwarn com.google.android.gms.fido.fido2.api.common.AuthenticationExtensions$Builder
-dontwarn com.google.android.gms.fido.fido2.api.common.AuthenticationExtensions
-dontwarn com.google.android.gms.fido.fido2.api.common.AuthenticatorAssertionResponse
-dontwarn com.google.android.gms.fido.fido2.api.common.AuthenticatorAttestationResponse
-dontwarn com.google.android.gms.fido.fido2.api.common.AuthenticatorErrorResponse
-dontwarn com.google.android.gms.fido.fido2.api.common.AuthenticatorSelectionCriteria$Builder
-dontwarn com.google.android.gms.fido.fido2.api.common.AuthenticatorSelectionCriteria
-dontwarn com.google.android.gms.fido.fido2.api.common.BrowserPublicKeyCredentialCreationOptions$Builder
-dontwarn com.google.android.gms.fido.fido2.api.common.BrowserPublicKeyCredentialCreationOptions
-dontwarn com.google.android.gms.fido.fido2.api.common.BrowserPublicKeyCredentialRequestOptions$Builder
-dontwarn com.google.android.gms.fido.fido2.api.common.BrowserPublicKeyCredentialRequestOptions
-dontwarn com.google.android.gms.fido.fido2.api.common.EC2Algorithm
-dontwarn com.google.android.gms.fido.fido2.api.common.ErrorCode
-dontwarn com.google.android.gms.fido.fido2.api.common.FidoAppIdExtension
-dontwarn com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialCreationOptions$Builder
-dontwarn com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialCreationOptions
-dontwarn com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialDescriptor
-dontwarn com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialParameters
-dontwarn com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialRequestOptions$Builder
-dontwarn com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialRequestOptions
-dontwarn com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialRpEntity
-dontwarn com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialType
-dontwarn com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialUserEntity
-dontwarn com.google.android.gms.fido.fido2.api.common.RSAAlgorithm
-dontwarn com.google.android.gms.fido.fido2.api.common.ResidentKeyRequirement
-dontwarn com.google.android.gms.tasks.OnFailureListener
-dontwarn com.google.android.gms.tasks.OnSuccessListener
-dontwarn com.google.android.gms.tasks.Task
-dontwarn org.mozilla.thirdparty.com.google.android.exoplayer2.BaseRenderer
-dontwarn org.mozilla.thirdparty.com.google.android.exoplayer2.DefaultLoadControl$Builder
-dontwarn org.mozilla.thirdparty.com.google.android.exoplayer2.DefaultLoadControl
-dontwarn org.mozilla.thirdparty.com.google.android.exoplayer2.ExoPlaybackException
-dontwarn org.mozilla.thirdparty.com.google.android.exoplayer2.ExoPlayer$Builder
-dontwarn org.mozilla.thirdparty.com.google.android.exoplayer2.ExoPlayer
-dontwarn org.mozilla.thirdparty.com.google.android.exoplayer2.Format
-dontwarn org.mozilla.thirdparty.com.google.android.exoplayer2.FormatHolder
-dontwarn org.mozilla.thirdparty.com.google.android.exoplayer2.LoadControl
-dontwarn org.mozilla.thirdparty.com.google.android.exoplayer2.PlaybackParameters
-dontwarn org.mozilla.thirdparty.com.google.android.exoplayer2.Player$EventListener
-dontwarn org.mozilla.thirdparty.com.google.android.exoplayer2.Renderer
-dontwarn org.mozilla.thirdparty.com.google.android.exoplayer2.Timeline$Period
-dontwarn org.mozilla.thirdparty.com.google.android.exoplayer2.Timeline$Window
-dontwarn org.mozilla.thirdparty.com.google.android.exoplayer2.Timeline
-dontwarn org.mozilla.thirdparty.com.google.android.exoplayer2.decoder.DecoderInputBuffer
-dontwarn org.mozilla.thirdparty.com.google.android.exoplayer2.source.MediaSource
-dontwarn org.mozilla.thirdparty.com.google.android.exoplayer2.source.MediaSourceEventListener
-dontwarn org.mozilla.thirdparty.com.google.android.exoplayer2.source.TrackGroup
-dontwarn org.mozilla.thirdparty.com.google.android.exoplayer2.source.TrackGroupArray
-dontwarn org.mozilla.thirdparty.com.google.android.exoplayer2.source.hls.HlsMediaSource$Factory
-dontwarn org.mozilla.thirdparty.com.google.android.exoplayer2.source.hls.HlsMediaSource
-dontwarn org.mozilla.thirdparty.com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection$Factory
-dontwarn org.mozilla.thirdparty.com.google.android.exoplayer2.trackselection.DefaultTrackSelector
-dontwarn org.mozilla.thirdparty.com.google.android.exoplayer2.trackselection.TrackSelection$Factory
-dontwarn org.mozilla.thirdparty.com.google.android.exoplayer2.trackselection.TrackSelection
-dontwarn org.mozilla.thirdparty.com.google.android.exoplayer2.trackselection.TrackSelectionArray
-dontwarn org.mozilla.thirdparty.com.google.android.exoplayer2.trackselection.TrackSelector
-dontwarn org.mozilla.thirdparty.com.google.android.exoplayer2.upstream.BandwidthMeter
-dontwarn org.mozilla.thirdparty.com.google.android.exoplayer2.upstream.DataSource$Factory
-dontwarn org.mozilla.thirdparty.com.google.android.exoplayer2.upstream.DefaultAllocator
-dontwarn org.mozilla.thirdparty.com.google.android.exoplayer2.upstream.DefaultBandwidthMeter$Builder
-dontwarn org.mozilla.thirdparty.com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
-dontwarn org.mozilla.thirdparty.com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
-dontwarn org.mozilla.thirdparty.com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
-dontwarn org.mozilla.thirdparty.com.google.android.exoplayer2.upstream.HttpDataSource$Factory
-dontwarn org.mozilla.thirdparty.com.google.android.exoplayer2.upstream.TransferListener
-dontwarn org.yaml.snakeyaml.LoaderOptions
-dontwarn org.yaml.snakeyaml.TypeDescription
-dontwarn org.yaml.snakeyaml.Yaml
-dontwarn org.yaml.snakeyaml.constructor.BaseConstructor
-dontwarn org.yaml.snakeyaml.constructor.Constructor
-dontwarn org.yaml.snakeyaml.error.YAMLException