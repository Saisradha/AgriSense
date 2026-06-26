package com.jo.agrisenseai;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ExpandableListView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import java.util.HashMap;
import java.util.Map;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AIAssistantFragment extends Fragment {

    private static final String PREFS_NAME         = "agrisense_prefs";
    private static final String KEY_CONVERSATION_ID = "current_conversation_id";

    // ── Views ──────────────────────────────────────────────────────────────────
    private RecyclerView         chatRecyclerView;
    private ChatAdapter          chatAdapter;
    private List<ChatMessage>    chatMessages;
    private EditText             chatInput;
    private FloatingActionButton btnSend;
    private FloatingActionButton fabMic;
    private ImageView            btnFolders;

    // Sidebar Views
    private DrawerLayout                 drawerLayout;
    private ExpandableListView           sidebarExpandableList;
    private SidebarExpandableListAdapter sidebarAdapter;
    private EditText                     searchChatsInput;
    private Button                       btnNewChat;
    private LinearLayout                 btnSettings;

    // Selection bar views
    private LinearLayout selectionBar;
    private TextView     selectionCount;
    private Button       btnSelectAll;
    private Button       btnDeleteSelected;
    private Button       btnCancelSelection;

    // ── Services ───────────────────────────────────────────────────────────────
    private AIService              aiService;
    private WeatherService         weatherService;
    private SpeechManager          speechManager;
    private TTSManager             ttsManager;
    private ChatRepository         chatRepository;
    private ConversationRepository conversationRepository;
    private FolderRepository       folderRepository;
    private Gson                   gson;

    private SensorData        latestSensorData;
    private ValueEventListener sensorListener;

    // ── State ──────────────────────────────────────────────────────────────────
    private long    currentConversationId = -1L;
    private boolean isListening           = false;
    private boolean isMuted               = false;

    // ── Launchers ──────────────────────────────────────────────────────────────

    private final ActivityResultLauncher<String> micPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    startListening();
                } else if (isAdded()) {
                    Toast.makeText(requireContext(),
                            getString(R.string.voice_status_mic_denied), Toast.LENGTH_SHORT).show();
                }
            });

    /** Launched when user taps the folder icon; returns a conversation to open. */
    private final ActivityResultLauncher<Intent> browserLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (!isAdded() || result.getData() == null) return;
                if (result.getResultCode() == Activity.RESULT_OK) {
                    long newConvId = result.getData().getLongExtra(
                            ConversationBrowserActivity.RESULT_CONV_ID, -1L);
                    boolean deleted = result.getData().getBooleanExtra(
                            ConversationBrowserActivity.RESULT_CONV_DELETED, false);

                    if (newConvId > 0) {
                        switchToConversation(newConvId);
                    } else if (deleted) {
                        // Current conversation deleted; find/create a valid one
                        currentConversationId = -1;
                        saveConversationId(-1);
                        initDefaultConversation();
                    }
                }
            });

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ai_assistant, container, false);

        // Chat views
        chatRecyclerView = view.findViewById(R.id.chatRecyclerView);
        chatInput        = view.findViewById(R.id.chatInput);
        btnSend          = view.findViewById(R.id.btnSend);
        fabMic           = view.findViewById(R.id.fabMicLaunch);
        btnFolders       = view.findViewById(R.id.btnFolders);

        // Selection bar views
        selectionBar       = view.findViewById(R.id.selectionBar);
        selectionCount     = view.findViewById(R.id.selectionCount);
        btnSelectAll       = view.findViewById(R.id.btnSelectAll);
        btnDeleteSelected  = view.findViewById(R.id.btnDeleteSelected);
        btnCancelSelection = view.findViewById(R.id.btnCancelSelection);

        // RecyclerView
        chatMessages = new ArrayList<>();
        chatAdapter  = new ChatAdapter(chatMessages);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        chatRecyclerView.setAdapter(chatAdapter);

        // ── Multi-select callbacks ──
        chatAdapter.setSelectionListener(new ChatAdapter.SelectionListener() {
            @Override
            public void onSelectionModeChanged(boolean active) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    selectionBar.setVisibility(active ? View.VISIBLE : View.GONE);
                    btnSend.setEnabled(!active);
                    fabMic.setEnabled(!active);
                    btnFolders.setEnabled(!active);
                    if (!active) {
                        selectionCount.setText("0 selected");
                        btnDeleteSelected.setEnabled(false);
                    }
                });
            }
            @Override
            public void onSelectionCountChanged(int count) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    selectionCount.setText(count + " selected");
                    btnDeleteSelected.setEnabled(count > 0);
                });
            }
        });

        btnDeleteSelected.setEnabled(false);
        btnSelectAll.setOnClickListener(v -> chatAdapter.selectAll());
        btnDeleteSelected.setOnClickListener(v -> showDeleteSelectedConfirmation());
        btnCancelSelection.setOnClickListener(v -> chatAdapter.exitSelectionMode());

        // ── Services ──
        aiService              = new AIService();
        weatherService         = new WeatherService();
        speechManager          = new SpeechManager(requireContext());
        ttsManager             = new TTSManager(requireContext(), () -> { /* TTS ready */ });
        chatRepository         = new ChatRepository(requireContext());
        conversationRepository = new ConversationRepository(requireContext());
        folderRepository       = new FolderRepository(requireContext());
        gson                   = new Gson();

        // ── Folder button (toggles sidebar drawer) ──
        btnFolders.setOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        // ── Sidebar views & adapter ──
        drawerLayout          = view.findViewById(R.id.drawerLayout);
        sidebarExpandableList = view.findViewById(R.id.sidebarExpandableList);
        searchChatsInput      = view.findViewById(R.id.searchChatsInput);
        btnNewChat            = view.findViewById(R.id.btnNewChat);
        btnSettings           = view.findViewById(R.id.btnSettings);

        sidebarAdapter = new SidebarExpandableListAdapter();
        sidebarExpandableList.setAdapter(sidebarAdapter);

        // Sidebar clicks
        sidebarExpandableList.setOnChildClickListener((parent1, v, groupPosition, childPosition, id) -> {
            ConversationEntity conv = (ConversationEntity) sidebarAdapter.getChild(groupPosition, childPosition);
            if (conv != null) {
                switchToConversation(conv.getId());
                drawerLayout.closeDrawer(GravityCompat.START);
            }
            return true;
        });

        btnNewChat.setOnClickListener(v -> {
            conversationRepository.ensureDefaultData(result -> {
                if (!isAdded()) return;
                long generalFolderId = result[0];
                conversationRepository.insertConversation(generalFolderId, "New Chat", newId -> {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        switchToConversation(newId);
                        loadSidebarData(null);
                        drawerLayout.closeDrawer(GravityCompat.START);
                    });
                });
            });
        });

        btnSettings.setOnClickListener(v -> {
            showSettingsDialog();
            drawerLayout.closeDrawer(GravityCompat.START);
        });

        // Real-time search chats
        searchChatsInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadSidebarData(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // ── Button listeners ──
        fabMic.setOnClickListener(v -> onMicClicked());
        btnSend.setOnClickListener(v -> handleSendText());

        // ── Load or create the default conversation ──
        long savedId = loadConversationId();
        if (savedId > 0) {
            currentConversationId = savedId;
            loadConversationHistory();
        } else {
            initDefaultConversation();
        }

        // Long-click listeners in sidebar for renaming/deleting folders & chats
        sidebarExpandableList.setOnItemLongClickListener((parent, view1, position, id) -> {
            long packedPosition = sidebarExpandableList.getExpandableListPosition(position);
            int itemType = ExpandableListView.getPackedPositionType(packedPosition);
            int groupPos = ExpandableListView.getPackedPositionGroup(packedPosition);
            int childPos = ExpandableListView.getPackedPositionChild(packedPosition);

            if (itemType == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
                ConversationEntity conv = (ConversationEntity) sidebarAdapter.getChild(groupPos, childPos);
                if (conv != null) {
                    showConversationContextMenu(view1, conv);
                }
                return true;
            } else if (itemType == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
                FolderEntity folder = (FolderEntity) sidebarAdapter.getGroup(groupPos);
                if (folder != null) {
                    showFolderContextMenu(view1, folder);
                }
                return true;
            }
            return false;
        });

        loadSidebarData(null);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        loadSidebarData(searchChatsInput != null ? searchChatsInput.getText().toString() : null);
        sensorListener = FirebaseHelper.getInstance().listenSensorData(data -> {
            if (!isAdded()) return;
            latestSensorData = data;
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        if (sensorListener != null)
            FirebaseHelper.getInstance().removeListener(FirebaseHelper.NODE_SENSOR_DATA, sensorListener);
        if (ttsManager != null) ttsManager.stop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (speechManager          != null) speechManager.shutdown();
        if (ttsManager             != null) ttsManager.shutdown();
        if (chatRepository         != null) chatRepository.shutdown();
        if (conversationRepository != null) conversationRepository.shutdown();
        if (folderRepository       != null) folderRepository.shutdown();
    }

    // ── Conversation management ────────────────────────────────────────────────

    /**
     * Ensures "General" folder + a default conversation exist.
     * Called on first launch or after current conversation is deleted.
     */
    private void initDefaultConversation() {
        conversationRepository.ensureDefaultData(result -> {
            if (!isAdded()) return;
            long convId = result[1];
            currentConversationId = convId;
            saveConversationId(convId);
            requireActivity().runOnUiThread(this::loadConversationHistory);
        });
    }

    /** Switches the chat view to a different conversation. */
    private void switchToConversation(long conversationId) {
        if (!isAdded()) return;
        currentConversationId = conversationId;
        saveConversationId(conversationId);

        if (ttsManager != null) ttsManager.stop();
        if (chatAdapter.isSelectionMode()) chatAdapter.exitSelectionMode();

        chatMessages.clear();
        chatAdapter.notifyDataSetChanged();
        loadConversationHistory();

        if (sidebarAdapter != null) {
            sidebarAdapter.setCurrentConversationId(conversationId);
        }
    }

    private void openConversationBrowser() {
        Intent intent = new Intent(requireContext(), ConversationBrowserActivity.class);
        intent.putExtra(ConversationBrowserActivity.EXTRA_CURRENT_CONV_ID, currentConversationId);
        browserLauncher.launch(intent);
    }

    // ── Chat history ───────────────────────────────────────────────────────────

    private void loadConversationHistory() {
        if (currentConversationId <= 0) return;
        chatRepository.getMessagesForConversation(currentConversationId, entities -> {
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (entities.isEmpty()) {
                    loadInitialMessages();
                } else {
                    chatMessages.clear();
                    for (ChatEntity entity : entities) {
                        List<String> checklistItems = null;
                        if (entity.getChecklistJson() != null && !entity.getChecklistJson().isEmpty()) {
                            try {
                                checklistItems = gson.fromJson(entity.getChecklistJson(), ArrayList.class);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        chatMessages.add(new ChatMessage(
                                entity.getText(), entity.getType(), false, checklistItems));
                    }
                    chatAdapter.notifyDataSetChanged();
                    scrollToLatest();
                }
            });
        });
    }

    private void loadInitialMessages() {
        addAndSaveMessage(new ChatMessage(getString(R.string.voice_greeting), ChatMessage.TYPE_AI));
    }

    private void addAndSaveMessage(ChatMessage message) {
        chatMessages.add(message);
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        scrollToLatest();

        String json = message.getChecklistItems() != null
                ? gson.toJson(message.getChecklistItems()) : null;
        chatRepository.insert(new ChatEntity(
                message.getText(),
                message.getType(),
                message.getTimestamp(),
                json,
                currentConversationId));
    }

    // ── SharedPreferences helpers ──────────────────────────────────────────────

    private long loadConversationId() {
        return requireContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getLong(KEY_CONVERSATION_ID, -1L);
    }

    private void saveConversationId(long id) {
        requireContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putLong(KEY_CONVERSATION_ID, id).apply();
    }

    // ── Voice / Speech ─────────────────────────────────────────────────────────

    private void onMicClicked() {
        if (chatAdapter.isSelectionMode()) { chatAdapter.exitSelectionMode(); return; }
        if (isListening) {
            speechManager.stop();
            setMicButtonListening(false);
            isListening = false;
            chatInput.setHint(getString(R.string.assistant_chat_hint));
            return;
        }
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
        } else {
            startListening();
        }
    }

    private void startListening() {
        isListening = true;
        setMicButtonListening(true);
        chatInput.setHint(getString(R.string.voice_status_listening));
        chatInput.setText("");

        speechManager.startListening(new SpeechManager.SpeechCallback() {
            @Override public void onReadyForSpeech() {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() ->
                        chatInput.setHint(getString(R.string.voice_status_listening)));
            }
            @Override public void onResult(String text) {
                isListening = false;
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    setMicButtonListening(false);
                    chatInput.setHint(getString(R.string.assistant_chat_hint));
                    chatInput.setText(text);
                    handleSendText();
                });
            }
            @Override public void onError(int errorCode) {
                isListening = false;
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    setMicButtonListening(false);
                    chatInput.setHint(getString(R.string.assistant_chat_hint));
                    String msg;
                    if (errorCode == android.speech.SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS)
                        msg = getString(R.string.voice_status_mic_denied);
                    else if (errorCode == android.speech.SpeechRecognizer.ERROR_NO_MATCH)
                        msg = "No speech detected. Try again.";
                    else
                        msg = "Speech recognition error. Try again.";
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void setMicButtonListening(boolean listening) {
        if (!isAdded()) return;
        fabMic.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(),
                listening ? R.color.status_critical : R.color.primary_green));
    }

    // ── Send message ───────────────────────────────────────────────────────────

    private void handleSendText() {
        if (chatAdapter.isSelectionMode()) { chatAdapter.exitSelectionMode(); return; }

        final String text = chatInput.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;

        // Auto-title: set conversation title from first user message
        boolean noUserMessageYet = true;
        for (ChatMessage m : chatMessages) {
            if (m.getType() == ChatMessage.TYPE_USER) { noUserMessageYet = false; break; }
        }
        if (noUserMessageYet && currentConversationId > 0) {
            // 1. Immediately save a fallback title from the message substring
            String initialTitle = text.length() > 50 ? text.substring(0, 47) + "…" : text;
            conversationRepository.updateConversationTitle(currentConversationId, initialTitle, unused -> {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() ->
                            loadSidebarData(searchChatsInput != null ? searchChatsInput.getText().toString() : null));
                }
            });

            // 2. Ask Gemini to generate a smart ChatGPT-style title asynchronously
            aiService.generateTitle(text, new AIServiceCallback() {
                @Override
                public void onSuccess(String generatedTitle) {
                    if (isAdded() && currentConversationId > 0) {
                        conversationRepository.updateConversationTitle(currentConversationId, generatedTitle, unused -> {
                            if (isAdded()) {
                                requireActivity().runOnUiThread(() ->
                                        loadSidebarData(searchChatsInput != null ? searchChatsInput.getText().toString() : null));
                            }
                        });
                    }
                }
                @Override
                public void onFailure(String errorMessage) {
                    // Fallback is already saved, so do nothing on failure
                    android.util.Log.e("AIAssistantFragment", "Title generation failed: " + errorMessage);
                }
            });
        }

        ChatMessage userMessage = new ChatMessage(text, ChatMessage.TYPE_USER);
        addAndSaveMessage(userMessage);
        chatInput.setText("");

        btnSend.setEnabled(false);
        fabMic.setEnabled(false);

        final ChatMessage loadingMessage =
                new ChatMessage("AgriSense AI is thinking…", ChatMessage.TYPE_AI, true);
        chatMessages.add(loadingMessage);
        final int loadingPos = chatMessages.size() - 1;
        chatAdapter.notifyItemInserted(loadingPos);
        scrollToLatest();

        FirebaseHelper.getInstance().getUnifiedTelemetry(new TelemetryCallback() {
            @Override
            public void onTelemetryLoaded(FarmTelemetry tempTelemetry) {
                weatherService.getWeatherData(tempTelemetry.getFarmName(), new WeatherCallback() {
                    @Override public void onSuccess(WeatherData wd) {
                        if (!isAdded()) return;
                        callGemini(buildFinal(tempTelemetry, wd), loadingMessage, loadingPos);
                    }
                    @Override public void onFailure(String e) {
                        if (!isAdded()) return;
                        callGemini(buildFinal(tempTelemetry, null), loadingMessage, loadingPos);
                    }
                });
            }
            @Override
            public void onTelemetryError(String dbError) {
                if (!isAdded()) return;
                weatherService.getWeatherData("Hyderabad,IN", new WeatherCallback() {
                    @Override public void onSuccess(WeatherData wd) {
                        callGemini(new FarmTelemetry(28.0, 65.0, 500.0, 80.0,
                                "OFF", "No irrigation needed", "Demo Farm", wd),
                                loadingMessage, loadingPos);
                    }
                    @Override public void onFailure(String e) {
                        callGemini(new FarmTelemetry(28.0, 65.0, 500.0, 80.0,
                                "OFF", "No irrigation needed", "Demo Farm", null),
                                loadingMessage, loadingPos);
                    }
                });
            }
        });
    }

    private FarmTelemetry buildFinal(FarmTelemetry src, WeatherData weather) {
        return new FarmTelemetry(src.getTemperature(), src.getHumidity(), src.getSoilMoisture(),
                src.getWaterLevel(), src.getPumpStatus(), src.getAiPrediction(),
                src.getFarmName(), weather);
    }

    private void callGemini(FarmTelemetry telemetry, ChatMessage loadingMessage, int loadingPos) {
        aiService.sendMessage(chatMessages, telemetry, new AIServiceCallback() {
            @Override
            public void onSuccess(String responseText) {
                if (!isAdded()) return;
                btnSend.setEnabled(true);
                fabMic.setEnabled(true);

                loadingMessage.setText(responseText);
                loadingMessage.setLoading(false);
                chatAdapter.notifyItemChanged(loadingPos);

                // Save AI response
                chatRepository.insert(new ChatEntity(
                        responseText, ChatMessage.TYPE_AI,
                        loadingMessage.getTimestamp(), null, currentConversationId));

                // Update conversation last message preview
                String preview = responseText.length() > 80
                        ? responseText.substring(0, 77) + "…" : responseText;
                conversationRepository.updateConversationLastMessage(
                        currentConversationId, preview, null);

                // Checklist
                List<String> checklistPoints = generateChecklistItems(telemetry);
                addAndSaveMessage(new ChatMessage("", ChatMessage.TYPE_CHECKLIST, false, checklistPoints));
                scrollToLatest();

                if (!isMuted) ttsManager.speak(responseText);
            }
            @Override
            public void onFailure(String errorMessage) {
                if (!isAdded()) return;
                btnSend.setEnabled(true);
                fabMic.setEnabled(true);
                loadingMessage.setText("Error: " + errorMessage);
                loadingMessage.setLoading(false);
                chatAdapter.notifyItemChanged(loadingPos);
                scrollToLatest();
            }
        });
    }

    // ── Checklist ──────────────────────────────────────────────────────────────

    private List<String> generateChecklistItems(FarmTelemetry telemetry) {
        List<String> items = new ArrayList<>();
        if (telemetry == null) {
            items.add("Soil moisture status: Not Available");
            items.add("Weather: Stable");
            items.add("Risk level: Unknown");
            return items;
        }
        SensorData sd = new SensorData(telemetry.getTemperature(), telemetry.getHumidity(),
                telemetry.getSoilMoisture(), 600.0, telemetry.getWaterLevel(), telemetry.getPumpStatus());
        AIEngine.analyze(sd);
        int m = toSoilMoisturePercent(telemetry.getSoilMoisture());

        if (m > 70)       items.add("Soil moisture is optimal (" + m + "%)");
        else if (m >= 40) items.add("Soil moisture is moderate (" + m + "%)");
        else              items.add("Soil moisture is low (" + m + "%)");

        WeatherData w = telemetry.getWeatherData();
        if (w != null)
            items.add("Weather: " + w.getDescription() + " (" + Math.round(w.getTemp()) + "°C)");
        else
            items.add("Weather: Stable (" + Math.round(telemetry.getTemperature()) + "°C)");

        items.add("Prediction: " + telemetry.getAiPrediction());
        return items;
    }

    private int toSoilMoisturePercent(double raw) {
        if (raw <= 0) return 0;
        if (raw <= 100) return Math.max(0, Math.min(100, (int) Math.round(raw)));
        return Math.max(0, Math.min(100, (int) Math.round(100 - (raw / 1023.0) * 100)));
    }

    private void scrollToLatest() {
        if (!chatMessages.isEmpty())
            chatRecyclerView.post(() -> chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1));
    }

    // ── Multi-select deletion ──────────────────────────────────────────────────

    private void showDeleteSelectedConfirmation() {
        if (!isAdded()) return;
        int count = chatAdapter.getSelectedCount();
        if (count == 0) return;
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Messages")
                .setMessage("Delete " + count + " selected message(s)? This cannot be undone.")
                .setPositiveButton("Delete", (d, w) -> performDeleteSelected())
                .setNegativeButton("Cancel", null)
                .setCancelable(true)
                .show();
    }

    private void performDeleteSelected() {
        if (!isAdded()) return;
        Set<Long> selectedTs = chatAdapter.getSelectedTimestamps();
        if (selectedTs.isEmpty()) { chatAdapter.exitSelectionMode(); return; }

        if (ttsManager != null) ttsManager.stop();

        chatRepository.deleteSelectedMessages(new ArrayList<>(selectedTs), unused -> {
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                for (int i = chatMessages.size() - 1; i >= 0; i--) {
                    if (selectedTs.contains(chatMessages.get(i).getTimestamp())) {
                        chatMessages.remove(i);
                        chatAdapter.notifyItemRemoved(i);
                    }
                }
                chatAdapter.exitSelectionMode();
                if (chatMessages.isEmpty()) loadInitialMessages();
                String msg = selectedTs.size() == 1 ? "1 message deleted."
                        : selectedTs.size() + " messages deleted.";
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
            });
        });
    }

    // ── Clear current conversation messages ────────────────────────────────────

    @SuppressWarnings("unused")
    private void clearCurrentConversation() {
        if (!isAdded() || currentConversationId <= 0) return;
        if (ttsManager != null) ttsManager.stop();
        chatRepository.clearConversationHistory(currentConversationId, unused -> {
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                chatMessages.clear();
                chatAdapter.notifyDataSetChanged();
                loadInitialMessages();
                Toast.makeText(requireContext(), "Conversation cleared.", Toast.LENGTH_SHORT).show();
            });
        });
    }

    // ── Public ─────────────────────────────────────────────────────────────────

    public void setMuted(boolean muted) {
        this.isMuted = muted;
        if (isMuted && ttsManager != null) ttsManager.stop();
    }

    public boolean isMuted() { return isMuted; }

    // ── Sidebar helpers ────────────────────────────────────────────────────────

    private void loadSidebarData(String searchQuery) {
        if (!isAdded()) return;
        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            conversationRepository.searchConversations(searchQuery.trim(), conversations -> {
                if (!isAdded()) return;
                folderRepository.getAllFolders(allFolders -> {
                    if (!isAdded()) return;
                    
                    Map<Long, List<ConversationEntity>> map = new HashMap<>();
                    for (ConversationEntity conv : conversations) {
                        List<ConversationEntity> list = map.get(conv.getFolderId());
                        if (list == null) {
                            list = new ArrayList<>();
                            map.put(conv.getFolderId(), list);
                        }
                        list.add(conv);
                    }
                    
                    List<FolderEntity> matchedFolders = new ArrayList<>();
                    for (FolderEntity f : allFolders) {
                        if (map.containsKey(f.getId())) {
                            matchedFolders.add(f);
                        }
                    }
                    
                    requireActivity().runOnUiThread(() -> {
                        sidebarAdapter.setData(matchedFolders, map);
                        sidebarAdapter.setCurrentConversationId(currentConversationId);
                        for (int i = 0; i < matchedFolders.size(); i++) {
                            sidebarExpandableList.expandGroup(i);
                        }
                    });
                });
            });
        } else {
            folderRepository.getAllFolders(allFolders -> {
                if (!isAdded()) return;
                conversationRepository.getAllConversations(allConvs -> {
                    if (!isAdded()) return;
                    
                    Map<Long, List<ConversationEntity>> map = new HashMap<>();
                    for (ConversationEntity conv : allConvs) {
                        List<ConversationEntity> list = map.get(conv.getFolderId());
                        if (list == null) {
                            list = new ArrayList<>();
                            map.put(conv.getFolderId(), list);
                        }
                        list.add(conv);
                    }
                    
                    requireActivity().runOnUiThread(() -> {
                        sidebarAdapter.setData(allFolders, map);
                        sidebarAdapter.setCurrentConversationId(currentConversationId);
                    });
                });
            });
        }
    }

    private void showSettingsDialog() {
        if (!isAdded()) return;
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_sidebar_settings, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        TextView languageValueText = dialogView.findViewById(R.id.dialogLanguageValueText);
        View languageCard = dialogView.findViewById(R.id.dialogLanguageCard);
        MaterialSwitch switchVoice = dialogView.findViewById(R.id.dialogSwitchVoiceEnabled);
        MaterialSwitch switchAutoSpeak = dialogView.findViewById(R.id.dialogSwitchAutoSpeak);
        View btnClose = dialogView.findViewById(R.id.dialogBtnClose);

        languageValueText.setText(LanguageManager.getSavedDisplayName(requireContext()));
        switchVoice.setChecked(VoicePreferenceManager.isVoiceEnabled(requireContext()));
        switchAutoSpeak.setChecked(VoicePreferenceManager.isAutoSpeak(requireContext()));

        languageCard.setOnClickListener(v -> {
            dialog.dismiss();
            showLanguageDialog();
        });

        switchVoice.setOnCheckedChangeListener((btn, checked) ->
                VoicePreferenceManager.setVoiceEnabled(requireContext(), checked));

        switchAutoSpeak.setOnCheckedChangeListener((btn, checked) ->
                VoicePreferenceManager.setAutoSpeak(requireContext(), checked));

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showLanguageDialog() {
        if (!isAdded()) return;
        int currentIndex = LanguageManager.getSavedLocaleIndex(requireContext());

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_select_language)
                .setSingleChoiceItems(
                        LanguageManager.DISPLAY_NAMES,
                        currentIndex,
                        (dialog, which) -> {
                            String selectedCode = LanguageManager.LOCALE_CODES[which];
                            LanguageManager.saveLanguage(requireContext(), selectedCode);
                            dialog.dismiss();
                            if (getActivity() != null) {
                                getActivity().recreate();
                            }
                        })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    // ── Sidebar context menu and dialog helpers ─────────────────────────────────

    private EditText buildEditText(String initialText, String hint) {
        EditText et = new EditText(requireContext());
        if (initialText != null) {
            et.setText(initialText);
            et.setSelection(initialText.length());
        }
        if (hint != null) {
            et.setHint(hint);
        }
        return et;
    }

    private View buildDialogView(EditText et) {
        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        int marginHorizontal = (int) (24 * getResources().getDisplayMetrics().density);
        int marginVertical = (int) (8 * getResources().getDisplayMetrics().density);
        lp.setMargins(marginHorizontal, marginVertical, marginHorizontal, marginVertical);
        et.setLayoutParams(lp);
        container.addView(et);
        return container;
    }

    private void showFolderContextMenu(View anchor, FolderEntity folder) {
        if (!isAdded()) return;
        PopupMenuHandler.showFolderMenu(requireContext(), anchor, folder, new PopupMenuHandler.FolderMenuListener() {
            @Override
            public void onRename(FolderEntity f) {
                showRenameFolderDialog(f);
            }

            @Override
            public void onDelete(FolderEntity f) {
                showDeleteFolderDialog(f);
            }
        });
    }

    private void showRenameFolderDialog(FolderEntity folder) {
        if (!isAdded()) return;
        DialogHandler.showRenameDialog(requireContext(), "Rename Folder", folder.getName(), "Folder Name", new DialogHandler.RenameCallback() {
            @Override
            public String onRename(String name) {
                if (name.isEmpty()) {
                    return "Folder name cannot be empty";
                }
                boolean exists = false;
                for (int i = 0; i < sidebarAdapter.getGroupCount(); i++) {
                    FolderEntity f = (FolderEntity) sidebarAdapter.getGroup(i);
                    if (f.getId() != folder.getId() && f.getName().equalsIgnoreCase(name)) {
                        exists = true;
                        break;
                    }
                }
                if (exists) {
                    return "Folder already exists";
                }
                folderRepository.renameFolder(folder.getId(), name, unused -> requireActivity().runOnUiThread(() -> {
                    loadSidebarData(searchChatsInput != null ? searchChatsInput.getText().toString() : null);
                }));
                return null;
            }
        });
    }

    private void showDeleteFolderDialog(FolderEntity folder) {
        if (!isAdded()) return;
        DialogHandler.showDeleteFolderConfirmation(requireContext(), folder.getName(), new DialogHandler.FolderDeleteCallback() {
            @Override
            public void onMoveToGeneral() {
                folderRepository.deleteFolder(folder.getId(), unused -> requireActivity().runOnUiThread(() -> {
                    loadSidebarData(searchChatsInput != null ? searchChatsInput.getText().toString() : null);
                    Toast.makeText(requireContext(), "Folder deleted. Chats moved to General.", Toast.LENGTH_SHORT).show();
                }));
            }

            @Override
            public void onDeleteAll() {
                conversationRepository.getConversationsByFolder(folder.getId(), convList -> {
                    boolean containsCurrent = false;
                    for (ConversationEntity conv : convList) {
                        if (conv.getId() == currentConversationId) {
                            containsCurrent = true;
                            break;
                        }
                    }
                    final boolean finalContainsCurrent = containsCurrent;
                    folderRepository.deleteFolderPermanently(folder.getId(), unused -> requireActivity().runOnUiThread(() -> {
                        loadSidebarData(searchChatsInput != null ? searchChatsInput.getText().toString() : null);
                        if (finalContainsCurrent) {
                            currentConversationId = -1L;
                            saveConversationId(-1L);
                            initDefaultConversation();
                        }
                        Toast.makeText(requireContext(), "Folder and all its chats permanently deleted.", Toast.LENGTH_SHORT).show();
                    }));
                });
            }
        });
    }

    private void showConversationContextMenu(View anchor, ConversationEntity conv) {
        if (!isAdded()) return;
        PopupMenuHandler.showConversationMenu(requireContext(), anchor, conv, new PopupMenuHandler.ConversationMenuListener() {
            @Override
            public void onOpen(ConversationEntity c) {
                switchToConversation(c.getId());
                drawerLayout.closeDrawer(GravityCompat.START);
            }

            @Override
            public void onRename(ConversationEntity c) {
                showRenameConversationDialog(c);
            }

            @Override
            public void onDelete(ConversationEntity c) {
                showDeleteConversationDialog(c);
            }
        });
    }

    private void showRenameConversationDialog(ConversationEntity conv) {
        if (!isAdded()) return;
        DialogHandler.showRenameDialog(requireContext(), "Rename Chat", conv.getTitle(), "Chat Title", new DialogHandler.RenameCallback() {
            @Override
            public String onRename(String title) {
                if (title.isEmpty()) {
                    return "Title cannot be empty";
                }
                conversationRepository.renameConversation(conv.getId(), title, unused -> requireActivity().runOnUiThread(() -> {
                    loadSidebarData(searchChatsInput != null ? searchChatsInput.getText().toString() : null);
                }));
                return null;
            }
        });
    }

    private void showMoveConversationDialog(ConversationEntity conv) {
        if (!isAdded()) return;
        folderRepository.getAllFolders(folderList -> requireActivity().runOnUiThread(() -> {
            List<FolderEntity> targets = new ArrayList<>(folderList);
            if (targets.isEmpty()) {
                Toast.makeText(requireContext(), "No folders available.", Toast.LENGTH_SHORT).show();
                return;
            }
            String[] names = new String[targets.size()];
            for (int i = 0; i < targets.size(); i++) names[i] = targets.get(i).getName();

            new AlertDialog.Builder(requireContext())
                    .setTitle("Move to Folder")
                    .setItems(names, (d, which) -> {
                        FolderEntity target = targets.get(which);
                        conversationRepository.moveConversation(conv.getId(), target.getId(), unused -> requireActivity().runOnUiThread(() -> {
                            loadSidebarData(searchChatsInput != null ? searchChatsInput.getText().toString() : null);
                            Toast.makeText(requireContext(), "Moved to " + target.getName(), Toast.LENGTH_SHORT).show();
                        }));
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }));
    }

    private void showDeleteConversationDialog(ConversationEntity conv) {
        if (!isAdded()) return;
        DialogHandler.showDeleteChatConfirmation(requireContext(), conv.getTitle(), new DialogHandler.DeleteConfirmCallback() {
            @Override
            public void onDeleteConfirm() {
                conversationRepository.deleteConversation(conv.getId(), unused -> requireActivity().runOnUiThread(() -> {
                    loadSidebarData(searchChatsInput != null ? searchChatsInput.getText().toString() : null);
                    if (conv.getId() == currentConversationId) {
                        currentConversationId = -1;
                        saveConversationId(-1);
                        initDefaultConversation();
                    }
                    Toast.makeText(requireContext(), "Chat deleted.", Toast.LENGTH_SHORT).show();
                }));
            }
        });
    }
}
