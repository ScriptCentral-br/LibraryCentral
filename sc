local UserInputService = game:GetService("UserInputService")
local TweenService = game:GetService("TweenService")
local RunService = game:GetService("RunService")
local LocalPlayer = game:GetService("Players").LocalPlayer
local Mouse = LocalPlayer:GetMouse()
local HttpService = game:GetService("HttpService")

local PARENT = (gethui and gethui()) or game:GetService('CoreGui')

local OrionLib = {
	Elements = {},
	ThemeObjects = {},
	Connections = {},
	Flags = {},
	Themes = {
		Default = {
			Main = Color3.fromRGB(15, 15, 15), -- Fundo bem escuro (quase preto)
			Second = Color3.fromRGB(32, 32, 32),
			Stroke = Color3.fromRGB(60, 60, 60),
			Divider = Color3.fromRGB(45, 45, 45),
			Text = Color3.fromRGB(240, 240, 240),
			TextDark = Color3.fromRGB(150, 150, 150),
			Accent = Color3.fromRGB(0, 110, 255),
			hover = Color3.fromRGB(40, 40, 40)
		}
	},
	SelectedTheme = "Default",
	Folder = nil,
	SaveCfg = false
}

-- Feather Icons Load
local Icons = {}
local Success, Response = pcall(function()
	Icons = HttpService:JSONDecode(game:HttpGetAsync("https://raw.githubusercontent.com/evoincorp/lucideblox/master/src/modules/util/icons.json")).icons
end)

if not Success then
	warn("\nOrion Library - Failed to load Feather Icons. Error code: " .. Response .. "\n")
end

local function GetIcon(IconName)
	if Icons[IconName] ~= nil then
		return Icons[IconName]
	else
		return nil
	end
end

local Orion = Instance.new("ScreenGui")
Orion.Name = "Orion"
Orion.Parent = PARENT
Orion.ZIndexBehavior = Enum.ZIndexBehavior.Sibling
Orion.ResetOnSpawn = false

for _, Interface in ipairs(PARENT:GetChildren()) do
	if Interface.Name == Orion.Name and Interface ~= Orion then
		Interface:Destroy()
	end
end

function OrionLib:IsRunning()
	return Orion.Parent == PARENT
end

local function AddConnection(Signal, Function)
	if (not OrionLib:IsRunning()) then
		return
	end
	local SignalConnect = Signal:Connect(Function)
	table.insert(OrionLib.Connections, SignalConnect)
	return SignalConnect
end

task.spawn(function()
	while (OrionLib:IsRunning()) do
		wait()
	end
	for _, Connection in next, OrionLib.Connections do
		Connection:Disconnect()
	end
end)

-- Sistema de Drag (Arrastar Janela)
local function MakeDraggable(DragPoint, Main)
	local Dragging, DragInput, MousePos, FramePos = false
	AddConnection(DragPoint.InputBegan, function(Input)
		if Input.UserInputType == Enum.UserInputType.MouseButton1 or Input.UserInputType == Enum.UserInputType.Touch then
			Dragging = true
			MousePos = Input.Position
			FramePos = Main.Position

			Input.Changed:Connect(function()
				if Input.UserInputState == Enum.UserInputState.End then
					Dragging = false
				end
			end)
		end
	end)
	AddConnection(DragPoint.InputChanged, function(Input)
		if Input.UserInputType == Enum.UserInputType.MouseMovement or Input.UserInputType == Enum.UserInputType.Touch then
			DragInput = Input
		end
	end)
	AddConnection(UserInputService.InputChanged, function(Input)
		if Input == DragInput and Dragging then
			local Delta = Input.Position - MousePos
			TweenService:Create(Main, TweenInfo.new(0.08, Enum.EasingStyle.Quad, Enum.EasingDirection.Out), {
				Position = UDim2.new(FramePos.X.Scale, FramePos.X.Offset + Delta.X, FramePos.Y.Scale, FramePos.Y.Offset + Delta.Y)
			}):Play()
		end
	end)
end

local function Create(Name, Properties, Children)
	local Object = Instance.new(Name)
	for i, v in next, Properties or {} do
		Object[i] = v
	end
	for i, v in next, Children or {} do
		v.Parent = Object
	end
	return Object
end

local function CreateElement(ElementName, ElementFunction)
	OrionLib.Elements[ElementName] = function(...)
		return ElementFunction(...)
	end
end

local function AddItemTable(Table, Item, Value)
	local Item = tostring(Item)
	local Count = 1
	while Table[Item] do
		Count = Count + 1
		Item = string.format('%s-%d', Item, Count)
	end
	Table[Item] = Value
end

local function MakeElement(ElementName, ...)
	return OrionLib.Elements[ElementName](...)
end

local function SetProps(Element, Props)
	table.foreach(Props, function(Property, Value)
		Element[Property] = Value
	end)
	return Element
end

local function SetChildren(Element, Children)
	table.foreach(Children, function(_, Child)
		Child.Parent = Element
	end)
	return Element
end

local function Round(Number, Factor)
	local Result = math.floor(Number/Factor + (math.sign(Number) * 0.5)) * Factor
	if Result < 0 then Result = Result + Factor end
	return Result
end

local function ReturnProperty(Object)
	if Object:IsA("Frame") or Object:IsA("TextButton") then
		return "BackgroundColor3"
	end
	if Object:IsA("ScrollingFrame") then
		return "ScrollBarImageColor3"
	end
	if Object:IsA("UIStroke") then
		return "Color"
	end
	if Object:IsA("TextLabel") or Object:IsA("TextBox") then
		return "TextColor3"
	end
	if Object:IsA("ImageLabel") or Object:IsA("ImageButton") then
		return "ImageColor3"
	end
end

local function AddThemeObject(Object, Type)
	if not OrionLib.ThemeObjects[Type] then
		OrionLib.ThemeObjects[Type] = {}
	end
	table.insert(OrionLib.ThemeObjects[Type], Object)
	Object[ReturnProperty(Object)] = OrionLib.Themes[OrionLib.SelectedTheme][Type]
	return Object
end

local function SetTheme()
	for Name, Type in pairs(OrionLib.ThemeObjects) do
		for _, Object in pairs(Type) do
			Object[ReturnProperty(Object)] = OrionLib.Themes[OrionLib.SelectedTheme][Name]
		end
	end
end

local function PackColor(Color)
	return {R = Color.R * 255, G = Color.G * 255, B = Color.B * 255}
end

local function UnpackColor(Color)
	return Color3.fromRGB(Color.R, Color.G, Color.B)
end

local function LoadCfg(Config)
	local Data = HttpService:JSONDecode(Config)
	table.foreach(Data, function(a,b)
		if OrionLib.Flags[a] then
			spawn(function()
				if OrionLib.Flags[a].Type == "Colorpicker" then
					OrionLib.Flags[a]:Set(UnpackColor(b))
				else
					OrionLib.Flags[a]:Set(b)
				end
			end)
		else
			warn("Orion Library Config Loader - Could not find ", a ,b)
		end
	end)
end

local function SaveCfg(Name)
	local Data = {}
	for i,v in pairs(OrionLib.Flags) do
		if v.Save then
			if v.Type == "Colorpicker" then
				Data[i] = PackColor(v.Value)
			else
				Data[i] = v.Value
			end
		end
	end
	if writefile then
		writefile(OrionLib.Folder .. "/" .. Name .. ".txt", tostring(HttpService:JSONEncode(Data)))
	end
end

local WhitelistedMouse = {Enum.UserInputType.MouseButton1, Enum.UserInputType.MouseButton2,Enum.UserInputType.MouseButton3}
local BlacklistedKeys = {Enum.KeyCode.Unknown,Enum.KeyCode.W,Enum.KeyCode.A,Enum.KeyCode.S,Enum.KeyCode.D,Enum.KeyCode.Up,Enum.KeyCode.Left,Enum.KeyCode.Down,Enum.KeyCode.Right,Enum.KeyCode.Slash,Enum.KeyCode.Tab,Enum.KeyCode.Backspace,Enum.KeyCode.Escape}

local function CheckKey(Table, Key)
	for _, v in next, Table do
		if v == Key then
			return true
		end
	end
end

-- Element Creators
CreateElement("Corner", function(Scale, Offset)
	return Create("UICorner", {
		CornerRadius = UDim.new(Scale or 0, Offset or 8) 
	})
end)

CreateElement("Stroke", function(Color, Thickness, Transparency)
	return Create("UIStroke", {
		Color = Color or Color3.fromRGB(255, 255, 255),
		Thickness = Thickness or 1,
		Transparency = Transparency or 0
	})
end)

CreateElement("List", function(Scale, Offset)
	return Create("UIListLayout", {
		SortOrder = Enum.SortOrder.LayoutOrder,
		Padding = UDim.new(Scale or 0, Offset or 6)
	})
end)

CreateElement("Padding", function(Bottom, Left, Right, Top)
	return Create("UIPadding", {
		PaddingBottom = UDim.new(0, Bottom or 4),
		PaddingLeft = UDim.new(0, Left or 4),
		PaddingRight = UDim.new(0, Right or 4),
		PaddingTop = UDim.new(0, Top or 4)
	})
end)

CreateElement("TFrame", function()
	return Create("Frame", {
		BackgroundTransparency = 1
	})
end)

CreateElement("Frame", function(Color)
	return Create("Frame", {
		BackgroundColor3 = Color or Color3.fromRGB(255, 255, 255),
		BorderSizePixel = 0
	})
end)

CreateElement("RoundFrame", function(Color, Scale, Offset)
	return Create("Frame", {
		BackgroundColor3 = Color or Color3.fromRGB(255, 255, 255),
		BorderSizePixel = 0
	}, {
		Create("UICorner", {
			CornerRadius = UDim.new(Scale or 0, Offset or 8)
		})
	})
end)

CreateElement("Button", function()
	return Create("TextButton", {
		Text = "",
		AutoButtonColor = false,
		BackgroundTransparency = 1,
		BorderSizePixel = 0
	})
end)

CreateElement("ScrollFrame", function(Color, Width)
	return Create("ScrollingFrame", {
		BackgroundTransparency = 1,
		MidImage = "rbxassetid://7445543667",
		BottomImage = "rbxassetid://7445543667",
		TopImage = "rbxassetid://7445543667",
		ScrollBarImageColor3 = Color,
		BorderSizePixel = 0,
		ScrollBarThickness = Width,
		CanvasSize = UDim2.new(0, 0, 0, 0)
	})
end)

CreateElement("Image", function(ImageID)
	local ImageNew = Create("ImageLabel", {
		Image = ImageID,
		BackgroundTransparency = 1
	})
	if GetIcon(ImageID) ~= nil then
		ImageNew.Image = GetIcon(ImageID)
	end
	return ImageNew
end)

CreateElement("ImageButton", function(ImageID)
	return Create("ImageButton", {
		Image = ImageID,
		BackgroundTransparency = 1
	})
end)

CreateElement("Label", function(Text, TextSize, Transparency)
	return Create("TextLabel", {
		Text = Text or "",
		TextColor3 = Color3.fromRGB(240, 240, 240),
		TextTransparency = Transparency or 0,
		TextSize = TextSize or 15,
		Font = Enum.Font.GothamMedium,
		RichText = true,
		BackgroundTransparency = 1,
		TextXAlignment = Enum.TextXAlignment.Left
	})
end)

local NotificationHolder = SetProps(SetChildren(MakeElement("TFrame"), {
	SetProps(MakeElement("List"), {
		HorizontalAlignment = Enum.HorizontalAlignment.Center,
		SortOrder = Enum.SortOrder.LayoutOrder,
		VerticalAlignment = Enum.VerticalAlignment.Bottom,
		Padding = UDim.new(0, 10)
	})
}), {
	Position = UDim2.new(1, -25, 1, -25),
	Size = UDim2.new(0, 300, 1, -25),
	AnchorPoint = Vector2.new(1, 1),
	Parent = Orion
})

function OrionLib:MakeNotification(NotificationConfig)
	spawn(function()
		NotificationConfig.Name = NotificationConfig.Name or "Notification"
		NotificationConfig.Content = NotificationConfig.Content or "Test"
		NotificationConfig.Image = NotificationConfig.Image or "rbxassetid://103928780885515"
		NotificationConfig.Time = NotificationConfig.Time or 15

		local NotificationParent = SetProps(MakeElement("TFrame"), {
			Size = UDim2.new(1, 0, 0, 0),
			AutomaticSize = Enum.AutomaticSize.Y,
			Parent = NotificationHolder
		})

		local NotificationFrame = SetChildren(SetProps(MakeElement("RoundFrame", Color3.fromRGB(25, 25, 25), 0, 10), {
			Parent = NotificationParent,
			Size = UDim2.new(1, 0, 0, 0),
			Position = UDim2.new(1, 50, 0, 0),
			BackgroundTransparency = 0.1,
			AutomaticSize = Enum.AutomaticSize.Y
		}), {
			MakeElement("Stroke", Color3.fromRGB(60, 60, 60), 1, 0.5),
			MakeElement("Padding", 12, 12, 12, 12),
			SetProps(MakeElement("Image", NotificationConfig.Image), {
				Size = UDim2.new(0, 24, 0, 24),
				ImageColor3 = OrionLib.Themes[OrionLib.SelectedTheme].Accent,
				Name = "Icon"
			}),
			SetProps(MakeElement("Label", NotificationConfig.Name, 16), {
				Size = UDim2.new(1, -34, 0, 24),
				Position = UDim2.new(0, 34, 0, 0),
				Font = Enum.Font.GothamBold,
				Name = "Title",
				TextColor3 = Color3.fromRGB(255, 255, 255)
			}),
			SetProps(MakeElement("Label", NotificationConfig.Content, 14), {
				Size = UDim2.new(1, 0, 0, 0),
				Position = UDim2.new(0, 0, 0, 30),
				Font = Enum.Font.Gotham,
				Name = "Content",
				RichText = true,
				AutomaticSize = Enum.AutomaticSize.Y,
				TextColor3 = Color3.fromRGB(200, 200, 200),
				TextWrapped = true
			})
		})

		TweenService:Create(NotificationFrame, TweenInfo.new(0.6, Enum.EasingStyle.Back, Enum.EasingDirection.Out), {Position = UDim2.new(0, 0, 0, 0)}):Play()

		wait(NotificationConfig.Time - 0.8)
		
		TweenService:Create(NotificationFrame, TweenInfo.new(0.5, Enum.EasingStyle.Back, Enum.EasingDirection.In), {Position = UDim2.new(1, 50, 0, 0)}):Play()
		TweenService:Create(NotificationFrame, TweenInfo.new(0.4, Enum.EasingStyle.Quad, Enum.EasingDirection.Out), {BackgroundTransparency = 1}):Play()
		TweenService:Create(NotificationFrame.UIStroke, TweenInfo.new(0.4, Enum.EasingStyle.Quad, Enum.EasingDirection.Out), {Transparency = 1}):Play()
		TweenService:Create(NotificationFrame.Title, TweenInfo.new(0.4, Enum.EasingStyle.Quad, Enum.EasingDirection.Out), {TextTransparency = 1}):Play()
		TweenService:Create(NotificationFrame.Content, TweenInfo.new(0.4, Enum.EasingStyle.Quad, Enum.EasingDirection.Out), {TextTransparency = 1}):Play()
		TweenService:Create(NotificationFrame.Icon, TweenInfo.new(0.4, Enum.EasingStyle.Quad, Enum.EasingDirection.Out), {ImageTransparency = 1}):Play()
		
		wait(0.5)
		NotificationFrame:Destroy()
	end)
end

function OrionLib:Init()
	if OrionLib.SaveCfg and (isfile and readfile) then
		pcall(function()
			if isfile(OrionLib.Folder .. "/" .. game.GameId .. ".txt") then
				LoadCfg(readfile(OrionLib.Folder .. "/" .. game.GameId .. ".txt"))
				OrionLib:MakeNotification({
					Name = "Configuration",
					Content = "Configuração carregada automaticamente.",
					Time = 5
				})
			end
		end)
	end
end

function OrionLib:MakeWindow(WindowConfig)
	local FirstTab = true
	local Minimized = false
	local UIHidden = false

	WindowConfig = WindowConfig or {}
	WindowConfig.Name = WindowConfig.Name or "ScriptCentral Universal"
	WindowConfig.ConfigFolder = WindowConfig.ConfigFolder or WindowConfig.Name
	WindowConfig.SaveConfig = WindowConfig.SaveConfig or false
	WindowConfig.HidePremium = WindowConfig.HidePremium or false
	if WindowConfig.IntroEnabled == nil then WindowConfig.IntroEnabled = true end
	WindowConfig.IntroText = WindowConfig.IntroText or "ScriptCentral Universal"
	WindowConfig.CloseCallback = WindowConfig.CloseCallback or function() end
	WindowConfig.ShowIcon = WindowConfig.ShowIcon or false
	WindowConfig.Icon = WindowConfig.Icon or "rbxassetid://103928780885515"
	WindowConfig.IntroIcon = WindowConfig.IntroIcon or "rbxassetid://103928780885515"
	WindowConfig.SearchBar = WindowConfig.SearchBar or nil
	OrionLib.Folder = WindowConfig.ConfigFolder
	OrionLib.SaveCfg = WindowConfig.SaveConfig

	if WindowConfig.SaveConfig then
		if (isfolder and makefolder) and not isfolder(WindowConfig.ConfigFolder) then
			makefolder(WindowConfig.ConfigFolder)
		end
	end

	local TabHolder = AddThemeObject(SetChildren(SetProps(MakeElement("ScrollFrame", Color3.fromRGB(255, 255, 255), 4),
		WindowConfig.SearchBar and {
			Size = UDim2.new(1, 0, 1, -90),
			Position = UDim2.new(0, 0, 0, 40)
		} or {
			Size = UDim2.new(1, 0, 1, -50)
		}),
		{
			MakeElement("List"),
			MakeElement("Padding", 8, 0, 0, 8)
		}), "Divider")

	AddConnection(TabHolder.UIListLayout:GetPropertyChangedSignal("AbsoluteContentSize"), function()
		TabHolder.CanvasSize = UDim2.new(0, 0, 0, TabHolder.UIListLayout.AbsoluteContentSize.Y + 16)
	end)

	local CloseBtn = SetChildren(SetProps(MakeElement("Button"), {
		Size = UDim2.new(0.5, 0, 1, 0),
		Position = UDim2.new(0.5, 0, 0, 0),
		BackgroundTransparency = 1
	}), {
		AddThemeObject(SetProps(MakeElement("Image", "rbxassetid://7072725342"), {
			Position = UDim2.new(0, 9, 0, 6),
			Size = UDim2.new(0, 18, 0, 18)
		}), "Text")
	})

	local MinimizeBtn = SetChildren(SetProps(MakeElement("Button"), {
		Size = UDim2.new(0.5, 0, 1, 0),
		BackgroundTransparency = 1
	}), {
		AddThemeObject(SetProps(MakeElement("Image", "rbxassetid://7072719338"), {
			Position = UDim2.new(0, 9, 0, 6),
			Size = UDim2.new(0, 18, 0, 18),
			Name = "Ico"
		}), "Text")
	})

	local DragPoint = SetProps(MakeElement("TFrame"), {
		Size = UDim2.new(1, 0, 0, 50)
	})

	local WindowStuff = AddThemeObject(SetChildren(SetProps(MakeElement("RoundFrame", Color3.fromRGB(255, 255, 255), 0, 12), {
		Size = UDim2.new(0, 170, 1, -50),
		Position = UDim2.new(0, 0, 0, 50)
	}), {
		AddThemeObject(SetProps(MakeElement("Frame"), {
			Size = UDim2.new(1, 0, 0, 1),
			Position = UDim2.new(0, 0, 0, 0)
		}), "Second"),
		AddThemeObject(SetProps(MakeElement("Frame"), {
			Size = UDim2.new(0, 1, 1, 0),
			Position = UDim2.new(1, -1, 0, 0)
		}), "Divider"),
		TabHolder,
		SetChildren(SetProps(MakeElement("TFrame"), {
			Size = UDim2.new(1, 0, 0, 50),
			Position = UDim2.new(0, 0, 1, -50)
		}), {
			AddThemeObject(SetProps(MakeElement("Frame"), {
				Size = UDim2.new(1, 0, 0, 1)
			}), "Divider"),
			-- CORREÇÃO DA FOTO DO PERFIL: GARANTIR COR BRANCA
			SetChildren(SetProps(MakeElement("TFrame"), {
				AnchorPoint = Vector2.new(0, 0.5),
				Size = UDim2.new(0, 32, 0, 32),
				Position = UDim2.new(0, 12, 0.5, 0)
			}), {
				SetChildren(SetProps(MakeElement("Image", "rbxthumb://type=AvatarHeadShot&id=" .. LocalPlayer.UserId .. "&w=150&h=150"), {
					Size = UDim2.new(1, 0, 1, 0),
					BackgroundTransparency = 1,
					BorderSizePixel = 0,
					ImageColor3 = Color3.fromRGB(255, 255, 255),
					ZIndex = 5 -- Garante que fica acima de qualquer sombra
				}), {
					MakeElement("Corner", 1)
				})
			}),
			AddThemeObject(SetProps(MakeElement("Label", LocalPlayer.DisplayName, WindowConfig.HidePremium and 14 or 13), {
				Size = UDim2.new(1, -60, 0, 13),
				Position = WindowConfig.HidePremium and UDim2.new(0, 54, 0, 19) or UDim2.new(0, 54, 0, 12),
				Font = Enum.Font.GothamBold,
				ClipsDescendants = true
			}), "Text"),
			AddThemeObject(SetProps(MakeElement("Label", "", 12), {
				Size = UDim2.new(1, -60, 0, 12),
				Position = UDim2.new(0, 54, 1, -25),
				Visible = not WindowConfig.HidePremium
			}), "TextDark")
		}),
	}), "Second")

	local Tabs = {}
	if WindowConfig.SearchBar then
		local SearchBox = Create("TextBox", {
			Size = UDim2.new(1, 0, 1, 0),
			BackgroundTransparency = 1,
			TextColor3 = Color3.fromRGB(255, 255, 255),
			PlaceholderColor3 = Color3.fromRGB(180,180,180),
			PlaceholderText = WindowConfig.SearchBar.Default or "🔍 Buscar...",
			Font = Enum.Font.GothamMedium,
			TextWrapped = true,
			Text = '',
			TextXAlignment = Enum.TextXAlignment.Left,
			TextSize = 14,
			ClearTextOnFocus = WindowConfig.SearchBar.ClearTextOnFocus or true
		})
		local TextboxActual = AddThemeObject(SearchBox, "Text")
		local SearchBar = AddThemeObject(SetChildren(SetProps(MakeElement("RoundFrame", Color3.fromRGB(255, 255, 255), 0, 8), {
			Parent = WindowStuff,
			Size = UDim2.new(0, 150, 0, 30),
			Position = UDim2.new(0, 10, 0, 10),
		}), {
			AddThemeObject(MakeElement("Stroke"), "Stroke"),
			MakeElement("Padding", 0, 8, 0, 0),
			TextboxActual
		}), "Main")

		local function SearchHandle()
			local Text = string.lower(SearchBox.Text)
			for i,v in pairs(Tabs) do
				if v:IsA('TextButton') then
					if string.find(string.lower(i), Text) then
						v.Visible = true
					else
						v.Visible = false
					end
				end
			end
		end
		AddConnection(TextboxActual:GetPropertyChangedSignal("Text"), SearchHandle)
	end

	local WindowName = AddThemeObject(SetProps(MakeElement("Label", WindowConfig.Name, 14), {
		Size = UDim2.new(1, -30, 2, 0),
		Position = UDim2.new(0, 25, 0, -24),
		Font = Enum.Font.GothamBlack,
		TextSize = 20
	}), "Text")

	local MainWindow = AddThemeObject(SetChildren(SetProps(MakeElement("RoundFrame", Color3.fromRGB(255, 255, 255), 0, 12), {
		Parent = Orion,
		Position = UDim2.new(0.5, 0, 0.5, 0),
		AnchorPoint = Vector2.new(0.5, 0.5), 
		Size = UDim2.new(0, 0, 0, 0), 
		ClipsDescendants = true,
		Visible = false 
	}), {
		SetChildren(SetProps(MakeElement("TFrame"), {
			Size = UDim2.new(1, 0, 0, 50),
			Name = "TopBar"
		}), {
			WindowName,
			AddThemeObject(SetProps(MakeElement("Frame"), { 
				Size = UDim2.new(1, 0, 0, 1),
				Position = UDim2.new(0, 0, 1, -1)
			}), "Divider"),
			AddThemeObject(SetChildren(SetProps(MakeElement("RoundFrame", Color3.fromRGB(255, 255, 255), 0, 8), {
				Size = UDim2.new(0, 70, 0, 30),
				Position = UDim2.new(1, -90, 0, 10)
			}), {
				AddThemeObject(MakeElement("Stroke"), "Stroke"),
				AddThemeObject(SetProps(MakeElement("Frame"), {
					Size = UDim2.new(0, 1, 1, 0),
					Position = UDim2.new(0.5, 0, 0, 0)
				}), "Divider"),
				CloseBtn,
				MinimizeBtn
			}), "Second"),
		}),
		DragPoint,
		WindowStuff,
		AddThemeObject(SetProps(MakeElement("Stroke", Color3.new(0,0,0), 3, 0.7),{}),"Stroke") 
	}), "Main")

	-- EFEITO DE FUNDO DEV/HACKER (CORRIGIDO PARA APARECER NO PRETO)
	local BackgroundPattern = Create("ImageLabel", {
		Name = "BackgroundPattern",
		Parent = MainWindow, 
		BackgroundTransparency = 1,
		Image = "rbxassetid://3517327730", -- Grid de pontos (Visível em Dark Mode)
		TileSize = UDim2.new(0, 25, 0, 25),
		ScaleType = Enum.ScaleType.Tile,
		Size = UDim2.new(1.5, 0, 1.5, 0), -- Tamanho para permitir movimento sem corte
		Position = UDim2.new(0, 0, 0, 0),
		ZIndex = 2, -- FORÇA aparecer acima da cor de fundo do Frame
		ImageColor3 = OrionLib.Themes[OrionLib.SelectedTheme].Accent, -- Usa a cor azul do tema
		ImageTransparency = 0.85 -- Transparência ajustada para não ofuscar o texto
	})
	
	-- Loop de Animação do Fundo
	spawn(function()
		local Tween = TweenService:Create(BackgroundPattern, TweenInfo.new(40, Enum.EasingStyle.Linear, Enum.EasingDirection.InOut, -1), {
			Position = UDim2.new(-0.5, 0, -0.5, 0) -- Move na diagonal
		})
		Tween:Play()
	end)

	if not WindowConfig.IntroEnabled then
		MainWindow.Visible = true
		TweenService:Create(MainWindow, TweenInfo.new(0.6, Enum.EasingStyle.Back, Enum.EasingDirection.Out), {
			Size = UDim2.new(0, 650, 0, 380) 
		}):Play()
	end

	if WindowConfig.ShowIcon then
		WindowName.Position = UDim2.new(0, 50, 0, -24)
		local WindowIcon = SetProps(MakeElement("Image", WindowConfig.Icon), {
			Size = UDim2.new(0, 20, 0, 20),
			Position = UDim2.new(0, 25, 0, 15)
		})
		WindowIcon.Parent = MainWindow.TopBar
	end

	MakeDraggable(DragPoint, MainWindow)

	local _currentKey = Enum.KeyCode.RightShift
	
	-- Ícone de abrir (Hub)
	local OpenButton = SetChildren(SetProps(MakeElement("ImageButton", "http://www.roblox.com/asset/?id=103928780885515"), {
		Position = UDim2.new(0.01, 0, 0.5, 0), 
		Size = UDim2.new(0, 45, 0, 45),
		Parent = Orion,
		Visible = false,
		BackgroundTransparency = 0.2,
		BackgroundColor3 = Color3.fromRGB(20, 20, 20)
	}), { 
		MakeElement("Corner", 0, 12),
		MakeElement("Stroke", Color3.fromRGB(60,60,60), 1)
	})

	-- CORREÇÃO FINAL: Lógica unificada para evitar "double trigger"
	local function MakeHubInteractable(Main)
		local Dragging, DragInput, MousePos, FramePos = false, nil, nil, nil
		local DragStart = Vector2.new()
		
		AddConnection(Main.InputBegan, function(Input)
			if Input.UserInputType == Enum.UserInputType.MouseButton1 or Input.UserInputType == Enum.UserInputType.Touch then
				Dragging = true
				MousePos = Input.Position
				FramePos = Main.Position
				DragStart = Input.Position
				
				-- Usar uma variável local para a conexão para poder desconectar corretamente
				local Connection
				Connection = Input.Changed:Connect(function()
					if Input.UserInputState == Enum.UserInputState.End then
						Dragging = false
						Connection:Disconnect() -- Desconecta para não acumular eventos
						
						-- Se moveu menos que 5 pixels, considera um CLIQUE
						if (Input.Position - DragStart).Magnitude < 5 then
							MainWindow.Visible = true
							Main.Visible = false
							
							-- Reset completo dos estados
							local Minimized = false -- Variável local aqui não afeta a externa, mas garante lógica visual
							WindowStuff.Visible = true
							MainWindow.ClipsDescendants = false -- FIX: Garante que está 'aberto' corretamente
							MinimizeBtn.Ico.Image = "rbxassetid://7072719338"
							
							MainWindow.Size = UDim2.new(0,0,0,0)
							TweenService:Create(MainWindow, TweenInfo.new(0.5, Enum.EasingStyle.Back, Enum.EasingDirection.Out), {Size = UDim2.new(0, 650, 0, 380)}):Play()
						end
					end
				end)
			end
		end)
		
		AddConnection(Main.InputChanged, function(Input)
			if Input.UserInputType == Enum.UserInputType.MouseMovement or Input.UserInputType == Enum.UserInputType.Touch then
				DragInput = Input
			end
		end)
		
		AddConnection(UserInputService.InputChanged, function(Input)
			if Input == DragInput and Dragging then
				local Delta = Input.Position - MousePos
				TweenService:Create(Main, TweenInfo.new(0.05, Enum.EasingStyle.Quad, Enum.EasingDirection.Out), {
					Position = UDim2.new(FramePos.X.Scale, FramePos.X.Offset + Delta.X, FramePos.Y.Scale, FramePos.Y.Offset + Delta.Y)
				}):Play()
			end
		end)
	end

	MakeHubInteractable(OpenButton)
	-- Removi a conexão antiga Button1Click para não conflitar com a lógica acima

	AddConnection(CloseBtn.MouseButton1Up, function()
		TweenService:Create(MainWindow, TweenInfo.new(0.4, Enum.EasingStyle.Back, Enum.EasingDirection.In), {Size = UDim2.new(0, 0, 0, 0)}):Play()
		wait(0.4)
		MainWindow.Visible = false
		UIHidden = true
		OpenButton.Visible = true
		WindowConfig.CloseCallback()
	end)

	AddConnection(UserInputService.InputBegan, function(Input)
		if Input.KeyCode == _currentKey then
			if MainWindow.Visible then
				TweenService:Create(MainWindow, TweenInfo.new(0.4, Enum.EasingStyle.Back, Enum.EasingDirection.In), {Size = UDim2.new(0, 0, 0, 0)}):Play()
				wait(0.4)
				MainWindow.Visible = false
				OpenButton.Visible = true 
			else
				OpenButton.Visible = false 
				MainWindow.Visible = true
				Minimized = false
				WindowStuff.Visible = true
				MinimizeBtn.Ico.Image = "rbxassetid://7072719338"
				TweenService:Create(MainWindow, TweenInfo.new(0.5, Enum.EasingStyle.Back, Enum.EasingDirection.Out), {Size = UDim2.new(0, 650, 0, 380)}):Play()
			end
		end
	end)

	AddConnection(MinimizeBtn.MouseButton1Up, function()
		if Minimized then
			TweenService:Create(MainWindow, TweenInfo.new(0.5, Enum.EasingStyle.Quint, Enum.EasingDirection.Out), {Size = UDim2.new(0, 650, 0, 380)}):Play()
			MinimizeBtn.Ico.Image = "rbxassetid://7072719338"
			wait(.02)
			MainWindow.ClipsDescendants = false
			WindowStuff.Visible = true
		else
			MainWindow.ClipsDescendants = true
			MinimizeBtn.Ico.Image = "rbxassetid://7072720870"
			TweenService:Create(MainWindow, TweenInfo.new(0.5, Enum.EasingStyle.Quint, Enum.EasingDirection.Out), {Size = UDim2.new(0, WindowName.TextBounds.X + 140, 0, 50)}):Play()
			wait(0.1)
			WindowStuff.Visible = false
		end
		Minimized = not Minimized
	end)

	local function LoadSequence()
